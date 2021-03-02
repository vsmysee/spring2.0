/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.Lifecycle;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * Abstract implementation of the ApplicationContext interface.
 * Doesn't mandate the type of storage used for configuration, but implements
 * common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed to
 * detect special beans defined in its internal bean factory: Therefore, this
 * class automatically registers BeanFactoryPostProcessors, BeanPostProcessors
 * and ApplicationListeners that are defined as beans in the context.
 *
 * <p>A MessageSource may also be supplied as a bean in the context, with
 * the name "messageSource"; else, message resolution is delegated to the
 * parent context. Furthermore, a multicaster for application events can
 * be supplied as "applicationEventMulticaster" bean in the context; else,
 * a SimpleApplicationEventMulticaster is used.
 *
 * <p>Implements resource loading through extending DefaultResourceLoader.
 * Consequently, treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the <code>getResourceByPath</code>
 * method is overwritten in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.ApplicationListener
 * @see #MESSAGE_SOURCE_BEAN_NAME
 * @see org.springframework.context.MessageSource
 * @see DelegatingMessageSource
 * @see #APPLICATION_EVENT_MULTICASTER_BEAN_NAME
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
 * @see #getResourceByPath(String)
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext, DisposableBean {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see org.springframework.context.MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Parent context */
	private ApplicationContext parent;

	/** BeanFactoryPostProcessors to apply on refresh */
	private final List beanFactoryPostProcessors = new ArrayList();

	/** Display name */
	private String displayName = getClass().getName() + ";hashCode=" + hashCode();

	/** System time in milliseconds when this context started */
	private long startupTime;

	/** Flag that indicates whether this context is currently active */
	private boolean active = false;

	/** Synchronization monitor for the "active" flag */
	private final Object activeMonitor = new Object();

	/** Synchronization monitor for the "refresh" and "destroy" */
	private final Object startupShutdownMonitor = new Object();

	/** Reference to the JVM shutdown hook, if registered */
	private Thread shutdownHook;

	/** ResourcePatternResolver used by this context */
	private ResourcePatternResolver resourcePatternResolver;

	/** MessageSource we delegate our implementation of this interface to */
	private MessageSource messageSource;

	/** Helper class used in event publishing */
	private ApplicationEventMulticaster applicationEventMulticaster;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	public AbstractApplicationContext() {
		this(null);
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(ApplicationContext parent) {
		if (parent != null) {
			setParent(parent);
		}
		this.resourcePatternResolver = getResourcePatternResolver();
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Return the parent context, or <code>null</code> if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	public ApplicationContext getParent() {
		return parent;
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	public long getStartupDate() {
		return startupTime;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	public void publishEvent(ApplicationEvent event) {
		Assert.notNull(event, "Event must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Publishing event in context [" + getDisplayName() + "]: " + event);
		}
		getApplicationEventMulticaster().multicastEvent(event);
		if (this.parent != null) {
			this.parent.publishEvent(event);
		}
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never <code>null</code>)
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}

	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
	}

	/**
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
	 */
	public List getBeanFactoryPostProcessors() {
		return beanFactoryPostProcessors;
	}


	public void refresh() throws BeansException, IllegalStateException {
		synchronized (this.startupShutdownMonitor) {
			this.startupTime = System.currentTimeMillis();

			synchronized (this.activeMonitor) {
				this.active = true;
			}

			// Tell subclass to refresh the internal bean factory.
			refreshBeanFactory();
			ConfigurableListableBeanFactory beanFactory = getBeanFactory();

			// Tell the internal bean factory to use the context's class loader.
			beanFactory.setBeanClassLoader(getClassLoader());

			// Populate the bean factory with context-specific resource editors.
			beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this));

			// Configure the bean factory with context semantics.
			beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
			beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
			beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
			beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
			beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

			// Allows post-processing of the bean factory in context subclasses.
			postProcessBeanFactory(beanFactory);

			// Invoke factory processors registered with the context instance.
			for (Iterator it = getBeanFactoryPostProcessors().iterator(); it.hasNext();) {
				BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();
				factoryProcessor.postProcessBeanFactory(beanFactory);
			}

			if (logger.isInfoEnabled()) {
				if (getBeanDefinitionCount() == 0) {
					logger.info("No beans defined in application context [" + getDisplayName() + "]");
				}
				else {
					logger.info(getBeanDefinitionCount() + " beans defined in application context [" + getDisplayName() + "]");
				}
			}

			try {
				// Invoke factory processors registered as beans in the context.
				invokeBeanFactoryPostProcessors();

				// Register bean processors that intercept bean creation.
				registerBeanPostProcessors();

				// Initialize message source for this context.
				initMessageSource();

				// Initialize event multicaster for this context.
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				onRefresh();

				// Check for listener beans and register them.
				registerListeners();

				// Instantiate singletons this late to allow them to access the message source.
				beanFactory.preInstantiateSingletons();

				// Last step: publish corresponding event.
				publishEvent(new ContextRefreshedEvent(this));
			}

			catch (BeansException ex) {
				// Destroy already created singletons to avoid dangling resources.
				beanFactory.destroySingletons();
				throw ex;
			}
		}
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is PathMatchingResourcePatternResolver,
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's <code>getResources</code> method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * Must be called before singleton instantiation.
	 */
	private void invokeBeanFactoryPostProcessors() throws BeansException {
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] factoryProcessorNames = getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessor that implement the Ordered
		// interface and those that do not.
		List orderedFactoryProcessors = new ArrayList();
		List nonOrderedFactoryProcessorNames = new ArrayList();
		for (int i = 0; i < factoryProcessorNames.length; i++) {
			if (Ordered.class.isAssignableFrom(getType(factoryProcessorNames[i]))) {
				orderedFactoryProcessors.add(getBean(factoryProcessorNames[i]));
			}
			else {
				nonOrderedFactoryProcessorNames.add(factoryProcessorNames[i]);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement Ordered.
		Collections.sort(orderedFactoryProcessors, new OrderComparator());
		for (Iterator it = orderedFactoryProcessors.iterator(); it.hasNext();) {
			BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();
			factoryProcessor.postProcessBeanFactory(getBeanFactory());
		}
		// Second, invoke all other BeanFactoryPostProcessors, one by one.
		for (Iterator it = nonOrderedFactoryProcessorNames.iterator(); it.hasNext();) {
			String factoryProcessorName = (String) it.next();
			((BeanFactoryPostProcessor) getBean(factoryProcessorName)).postProcessBeanFactory(getBeanFactory());
		}
	}

	/**
	 * Instantiate and invoke all registered BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 */
	private void registerBeanPostProcessors() throws BeansException {
		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		final int beanProcessorTargetCount = getBeanFactory().getBeanPostProcessorCount() + 1 +
				getBeanNamesForType(BeanPostProcessor.class, true, false).length;
		getBeanFactory().addBeanPostProcessor(new BeanPostProcessorChecker(beanProcessorTargetCount));

		// Actually fetch and register the BeanPostProcessor beans.
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean post-processors apply to them!
		Map beanProcessorMap = getBeansOfType(BeanPostProcessor.class, true, false);
		List beanProcessors = new ArrayList(beanProcessorMap.values());
		Collections.sort(beanProcessors, new OrderComparator());
		for (Iterator it = beanProcessors.iterator(); it.hasNext();) {
			getBeanFactory().addBeanPostProcessor((BeanPostProcessor) it.next());
		}
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	private void initMessageSource() throws BeansException {
		if (containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			this.messageSource = (MessageSource) getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isInfoEnabled()) {
				logger.info("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			DelegatingMessageSource dms = new DelegatingMessageSource();
			dms.setParentMessageSource(getInternalParentMessageSource());
			this.messageSource = dms;
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate MessageSource with name '" + MESSAGE_SOURCE_BEAN_NAME +
						"': using default [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	private void initApplicationEventMulticaster() throws BeansException {
		if (containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			this.applicationEventMulticaster = (ApplicationEventMulticaster)
					getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isInfoEnabled()) {
				logger.info("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		else {
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster();
			if (logger.isInfoEnabled()) {
				logger.info("Unable to locate ApplicationEventMulticaster with name '" +
						APPLICATION_EVENT_MULTICASTER_BEAN_NAME +
						"': using default [" + this.applicationEventMulticaster + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * @throws org.springframework.beans.BeansException in case of errors during refresh
	 * @see #refresh
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 */
	private void registerListeners() throws BeansException {
		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		Collection listeners = getBeansOfType(ApplicationListener.class, true, false).values();
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			addListener((ApplicationListener) it.next());
		}
	}

	/**
	 * Subclasses can invoke this method to register a listener.
	 * Any beans in the context that are listeners are automatically added.
	 * @param listener the listener to register
	 */
	protected void addListener(ApplicationListener listener) {
		getApplicationEventMulticaster().addApplicationListener(listener);
	}


	/**
	 * Register a shutdown hook with the JVM runtime, closing this context
	 * on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to <code>doClose()</code> for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see #close()
	 * @see #doClose()
	 */
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread() {
				public void run() {
					doClose();
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * DisposableBean callback for destruction of this instance.
	 * Only called when the ApplicationContext itself is running
	 * as a bean in another BeanFactory or ApplicationContext,
	 * which is rather unusual.
	 * <p>The <code>close</code> method is the native way to
	 * shut down an ApplicationContext.
	 * @see #close()
	 * @see org.springframework.beans.factory.access.SingletonBeanFactoryLocator
	 */
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to <code>doClose()</code> for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both <code>close()</code> and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	protected void doClose() {
		if (isActive()) {
			if (logger.isInfoEnabled()) {
				logger.info("Closing application context [" + getDisplayName() + "]");
			}
			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.error("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}
			// Destroy all cached singletons in this context, invoking
			// DisposableBean.destroy and/or the specified "destroy-method".
			getBeanFactory().destroySingletons();
			closeBeanFactory();
			onClose();
			synchronized (this.activeMonitor) {
				this.active = false;
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * Called at the end of <code>doClose</code>'s shutdown procedure.
	 * @see #doClose
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	public boolean isActive() {
		synchronized (this.activeMonitor) {
			return this.active;
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public Object getBean(String name, Class requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	public Class getType(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getType(name);
	}

	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public boolean containsBeanDefinition(String name) {
		return getBeanFactory().containsBeanDefinition(name);
	}

	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	public String[] getBeanNamesForType(Class type) {
		return getBeanFactory().getBeanNamesForType(type);
	}

	public String[] getBeanNamesForType(Class type, boolean includePrototypes, boolean allowEagerInit) {
		return getBeanFactory().getBeanNamesForType(type, includePrototypes, allowEagerInit);
	}

	public Map getBeansOfType(Class type) throws BeansException {
		return getBeanFactory().getBeansOfType(type);
	}

	public Map getBeansOfType(Class type, boolean includePrototypes, boolean allowEagerInit)
			throws BeansException {

		return getBeanFactory().getBeansOfType(type, includePrototypes, allowEagerInit);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext) ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : (BeanFactory) getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never <code>null</code>)
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext) ?
		    ((AbstractApplicationContext) getParent()).messageSource : getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	public void start() {
		Iterator it = getLifecycleBeans().iterator();
		while (it.hasNext()) {
			Lifecycle lifecycle = (Lifecycle) it.next();
			if (!lifecycle.isRunning()) {
				lifecycle.start();
			}
		}
	}

	public void stop() {
		Iterator it = getLifecycleBeans().iterator();
		while (it.hasNext()) {
			Lifecycle lifecycle = (Lifecycle) it.next();
			if (lifecycle.isRunning()) {
				lifecycle.stop();
			}
		}
	}

	public boolean isRunning() {
		Iterator it = getLifecycleBeans().iterator();
		while (it.hasNext()) {
			Lifecycle lifecycle = (Lifecycle) it.next();
			if (!lifecycle.isRunning()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return a Collection of all singleton beans that implement the
	 * Lifecycle interface in this context.
	 */
	protected Collection getLifecycleBeans() {
		return getBeanFactory().getBeansOfType(Lifecycle.class, false, false).values();
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by <code>refresh()</code> before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws org.springframework.beans.BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 * @see #refresh()
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * The method is invoked by <code>close()</code> after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 * @see #refresh()
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never <code>null</code>)
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if <code>refresh</code> has never been called) or if the context has been
	 * closed already
	 * @see #refresh()
	 */
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(": ");
		sb.append("display name [").append(this.displayName).append("]; ");
		sb.append("startup date [").append(new Date(this.startupTime)).append("]; ");
		if (this.parent == null) {
			sb.append("root of context hierarchy");
		}
		else {
			sb.append("child of [").append(this.parent).append(']');
		}
		return sb.toString();
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private class BeanPostProcessorChecker implements BeanPostProcessor {

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(int beanPostProcessorTargetCount) {
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (getBeanFactory().getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' is not eligible for getting processed by all " +
							"BeanPostProcessors (for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}
	}

}
