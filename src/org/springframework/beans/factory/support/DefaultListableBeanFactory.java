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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the ListableBeanFactory and BeanDefinitionRegistry
 * interfaces: a full-fledged bean factory based on bean definitions.
 *
 * <p>Typical usage is registering all bean definitions first (possibly read
 * from a bean definition file), before accessing beans. Bean definition lookup
 * is therefore an inexpensive operation in a local bean definition table.
 *
 * <p>Can be used as a standalone bean factory, or as a superclass for custom
 * bean factories. Note that readers for specific bean definition formats are
 * typically implemented separately rather than as bean factory subclasses.
 *
 * <p>For an alternative implementation of the ListableBeanFactory interface,
 * have a look at StaticListableBeanFactory, which manages existing bean
 * instances rather than creating new ones based on bean definitions.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 16 April 2001
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see StaticListableBeanFactory
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
    implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

	/** Whether to allow re-registration of a different definition with the same name */
	private boolean allowBeanDefinitionOverriding = true;

	/** Whether to allow eager class loading even for lazy-init beans */
	private boolean allowEagerClassLoading = true;

	/** Map of bean definition objects, keyed by bean name */
	private final Map beanDefinitionMap = new HashMap();

	/** List of bean definition names, in registration order */
	private final List beanDefinitionNames = new ArrayList();


	/**
	 * Create a new DefaultListableBeanFactory.
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * Create a new DefaultListableBeanFactory with the given parent.
	 */
	public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}


	/**
	 * Set whether it should be allowed to override bean definitions by registering
	 * a different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. Default is "true".
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Set whether the factory is allowed to eagerly load bean classes
	 * even for bean definitions that are marked as "lazy-init".
	 * <p>Default is "true". Turn this flag off to suppress class loading
	 * for lazy-init beans unless such a bean is explicitly requested.
	 * In particular, by-type lookups will then simply ignore bean definitions
	 * without resolved class name, instead of loading the bean classes on
	 * demand just to perform a type check.
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#setLazyInit
	 */
	public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
		this.allowEagerClassLoading = allowEagerClassLoading;
	}


	public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
		super.copyConfigurationFrom(otherFactory);
		if (otherFactory instanceof DefaultListableBeanFactory) {
			DefaultListableBeanFactory otherListableFactory = (DefaultListableBeanFactory) otherFactory;
			this.allowBeanDefinitionOverriding = otherListableFactory.allowBeanDefinitionOverriding;
			this.allowEagerClassLoading = otherListableFactory.allowEagerClassLoading;
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public boolean containsBeanDefinition(String beanName) {
		return this.beanDefinitionMap.containsKey(beanName);
	}

	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	public String[] getBeanDefinitionNames() {
		return StringUtils.toStringArray(this.beanDefinitionNames);
	}

	public String[] getBeanNamesForType(Class type) {
		return getBeanNamesForType(type, true, true);
	}

	public String[] getBeanNamesForType(Class type, boolean includePrototypes, boolean allowEagerInit) {
		List result = new ArrayList();

		// Check all bean definitions.
		for (Iterator it = this.beanDefinitionNames.iterator(); it.hasNext();) {
			String beanName = (String) it.next();
			// Only consider bean as eligible if the bean name
			// is not defined as alias for some other bean.
			if (!isAlias(beanName)) {
				RootBeanDefinition rbd = getMergedBeanDefinition(beanName, false);
				// Only check bean definition if it is complete.
				if (!rbd.isAbstract() &&
						(allowEagerInit || rbd.hasBeanClass() || !rbd.isLazyInit() || this.allowEagerClassLoading)) {
					// In case of FactoryBean, match object created by FactoryBean.
					try {
						Class beanClass = resolveBeanClass(rbd, beanName);
						boolean isFactoryBean = (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass));
						if (isFactoryBean || rbd.getFactoryBeanName() != null) {
							if (allowEagerInit && (includePrototypes || isSingleton(beanName)) &&
									isBeanTypeMatch(beanName, type)) {
								result.add(beanName);
								// Match found for this bean: do not match FactoryBean itself anymore.
								continue;
							}
							// We're done for anything but a full FactoryBean.
							if (!isFactoryBean) {
								continue;
							}
							// In case of FactoryBean, try to match FactoryBean itself next.
							beanName = FACTORY_BEAN_PREFIX + beanName;
						}
						// Match raw bean instance (might be raw FactoryBean).
						if ((includePrototypes || rbd.isSingleton()) && isBeanTypeMatch(beanName, type)) {
							result.add(beanName);
						}
					}
					catch (CannotLoadBeanClassException ex) {
						if (rbd.isLazyInit()) {
							if (logger.isDebugEnabled()) {
								logger.debug("Ignoring bean class loading failure for lazy-init bean '" + beanName + "'", ex);
							}
						}
						else {
							throw ex;
						}
					}
				}
			}
		}

		// Check singletons too, to catch manually registered singletons.
		String[] singletonNames = getSingletonNames();
		for (int i = 0; i < singletonNames.length; i++) {
			String beanName = singletonNames[i];
			// Only check if manually registered.
			if (!containsBeanDefinition(beanName)) {
				// In case of FactoryBean, match object created by FactoryBean.
				if (isFactoryBean(beanName)) {
					if ((includePrototypes || isSingleton(beanName)) && isBeanTypeMatch(beanName, type)) {
						result.add(beanName);
						// Match found for this bean: do not match FactoryBean itself anymore.
						continue;
					}
					// In case of FactoryBean, try to match FactoryBean itself next.
					beanName = FACTORY_BEAN_PREFIX + beanName;
				}
				// Match raw bean instance (might be raw FactoryBean).
				if (isBeanTypeMatch(beanName, type)) {
					result.add(beanName);
				}
			}
		}

		return StringUtils.toStringArray(result);
	}

	public Map getBeansOfType(Class type) throws BeansException {
		return getBeansOfType(type, true, true);
	}

	public Map getBeansOfType(Class type, boolean includePrototypes, boolean allowEagerInit)
			throws BeansException {

		String[] beanNames = getBeanNamesForType(type, includePrototypes, allowEagerInit);
		Map result = CollectionFactory.createLinkedMapIfPossible(beanNames.length);
		for (int i = 0; i < beanNames.length; i++) {
			String beanName = beanNames[i];
			try {
				result.put(beanName, getBean(beanName));
			}
			catch (BeanCreationException ex) {
				if (ex.contains(BeanCurrentlyInCreationException.class)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to currently created bean '" + beanName + "': " + ex.getMessage());
					}
					// Ignore: indicates a circular reference when autowiring constructors.
					// We want to find matches other than the currently created bean itself.
				}
				else {
					throw ex;
				}
			}
		}
		return result;
	}

	/**
	 * Check whether the specified bean matches the given type.
	 * @param beanName the name of the bean to check
	 * @param type the type to check for
	 * @return whether the bean matches the given type
	 * @see #getType
	 */
	private boolean isBeanTypeMatch(String beanName, Class type) {
		if (type == null) {
			return true;
		}
		Class beanType = getType(beanName);
		return (beanType != null && type.isAssignableFrom(beanType));
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory interface
	//---------------------------------------------------------------------

	public void preInstantiateSingletons() throws BeansException {
		if (logger.isInfoEnabled()) {
			logger.info("Pre-instantiating singletons in factory [" + this + "]");
		}
		for (Iterator it = this.beanDefinitionNames.iterator(); it.hasNext();) {
			String beanName = (String) it.next();
			if (!containsSingleton(beanName) && containsBeanDefinition(beanName)) {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
					Class beanClass = resolveBeanClass(bd, beanName);
					if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass)) {
						getBean(FACTORY_BEAN_PREFIX + beanName);
					}
					else {
						getBean(beanName);
					}
				}
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry interface
	//---------------------------------------------------------------------

	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		Assert.hasText(beanName, "Bean name must not be empty");
		Assert.notNull(beanDefinition, "Bean definition must not be null");

		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		Object oldBeanDefinition = this.beanDefinitionMap.get(beanName);
		if (oldBeanDefinition != null) {
			if (!this.allowBeanDefinitionOverriding) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Cannot register bean definition [" + beanDefinition + "] for bean '" + beanName +
						"': there's already [" + oldBeanDefinition + "] bound");
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Overriding bean definition for bean '" + beanName +
							"': replacing [" + oldBeanDefinition + "] with [" + beanDefinition + "]");
				}
			}
		}
		else {
			this.beanDefinitionNames.add(beanName);
		}
		this.beanDefinitionMap.put(beanName, beanDefinition);

		// Remove corresponding bean from singleton cache, if any.
		// Shouldn't usually be necessary, rather just meant for overriding
		// a context's default beans (e.g. the default StaticMessageSource
		// in a StaticApplicationContext).
		removeSingleton(beanName);
	}


	//---------------------------------------------------------------------
	// Implementation of superclass abstract methods
	//---------------------------------------------------------------------

	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		BeanDefinition bd = (BeanDefinition) this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("No bean named '" + beanName + "' found in " + toString());
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		return bd;
	}

	protected Map findMatchingBeans(Class requiredType) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this, requiredType);
	}


	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(" defining beans [");
		sb.append(StringUtils.arrayToDelimitedString(getBeanDefinitionNames(), ","));
		sb.append("]; ");
		if (getParentBeanFactory() == null) {
			sb.append("root of BeanFactory hierarchy");
		}
		else {
			sb.append("parent: " + getParentBeanFactory());
		}
		return sb.toString();
	}

}
