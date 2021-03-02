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

package org.springframework.web.context.support;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.RequestScope;
import org.springframework.web.context.request.SessionScope;

/**
 * Static WebApplicationContext implementation for testing.
 * Not for use in production applications.
 *
 * <p>Implements the ConfigurableWebApplicationContext interface to allow for
 * direct replacement of an XmlWebApplicationContext, despite not actually
 * supporting loading from a context config location.
 *
 * <p>Interprets resource paths as servlet context resources, that is, as paths beneath
 * the web application root. Absolute paths, for example for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by AbstractApplicationContext.
 *
 * <p>In addition to the special beans detected by AbstractApplicationContext,
 * this class detects a ThemeSource bean in the context, with the name "themeSource".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.ui.context.ThemeSource
 */
public class StaticWebApplicationContext extends StaticApplicationContext
		implements ConfigurableWebApplicationContext, ThemeSource {

	private ServletContext servletContext;

	private ServletConfig servletConfig;

	private String namespace;

	private ThemeSource themeSource;


	public StaticWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
	}

	/**
	 * Set the ServletContext that this WebApplicationContext runs in.
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	public ServletConfig getServletConfig() {
		return servletConfig;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	protected String getNamespace() {
		return this.namespace;
	}

	/**
	 * The {@link org.springframework.web.context.support.StaticWebApplicationContext} class does not support this method.
	 * @throws UnsupportedOperationException <b>always</b>
	 */
	public void setConfigLocations(String[] configLocations) {
		throw new UnsupportedOperationException("StaticWebApplicationContext does not support configLocations");
	}


	/**
	 * Register ServletContextAwareProcessor.
	 * @see org.springframework.web.context.support.ServletContextAwareProcessor
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.registerScope(SCOPE_REQUEST, new RequestScope());
		beanFactory.registerScope(SCOPE_SESSION, new SessionScope(false));
		beanFactory.registerScope(SCOPE_GLOBAL_SESSION, new SessionScope(true));

		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
	}

	/**
	 * This implementation supports file paths beneath the root of the ServletContext.
	 * @see org.springframework.web.context.support.ServletContextResource
	 */
	protected Resource getResourceByPath(String path) {
		return new ServletContextResource(this.servletContext, path);
	}

	/**
	 * This implementation supports pattern matching in unexpanded WARs too.
	 * @see org.springframework.web.context.support.ServletContextResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	/**
	 * Initialize the theme capability.
	 */
	protected void onRefresh() {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

}
