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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.util.StringUtils;

/**
 * Implementation of the HandlerMapping interface to map from URLs to beans with names
 * that start with a slash ("/"), similar to how Struts maps URLs to action names.
 * This is the default implementation used by the DispatcherServlet, but somewhat naive.
 * A SimpleUrlHandlerMapping or a custom handler mapping should be used by preference.
 *
 * <p>The mapping is from URL to bean name. Thus an incoming URL "/foo" would map
 * to a handler named "/foo", or to "/foo /foo2" in case of multiple mappings to
 * a single handler. Note: In XML definitions, you'll need to use an alias
 * name="/foo" in the bean definition, as the XML id may not contain slashes.
 *
 * <p>Supports direct matches (given "/test" -> registered "/test") and "*" matches
 * (given "/test" -> registered "/t*"). Note that the default is to map within the
 * current servlet mapping if applicable; see alwaysUseFullPath property for details.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setAlwaysUseFullPath
 * @see SimpleUrlHandlerMapping
 */
public class BeanNameUrlHandlerMapping extends AbstractUrlHandlerMapping {
	
	/**
	 * Calls the <code>detectHandlers()</code> method in addition
	 * to the superclass's initialization.
	 * @see #detectHandlers()
	 */
	public void initApplicationContext() throws ApplicationContextException {
		super.initApplicationContext();
		detectHandlers();
	}

	/**
	 * Register all handlers found in the current ApplicationContext.
	 * Any bean whose name appears to be a URL is considered a handler.
	 * @throws org.springframework.beans.BeansException if the handler couldn't be registered
	 * @see #determineUrlsForHandler
	 */
	protected void detectHandlers() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for URL mappings in application context: " + getApplicationContext());
		}
		String[] beanNames = getApplicationContext().getBeanDefinitionNames();

		// Take any bean name or alias that begins with a slash.
		for (int i = 0; i < beanNames.length; i++) {
			String beanName = beanNames[i];
			String[] urls = determineUrlsForHandler(beanName);
			if (urls.length > 0) {
				// URL paths found: Let's consider it a handler.
				registerHandler(urls, beanName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Rejected bean name '" + beanNames[i] + "': no URL paths identified");
				}
			}
		}
	}

	/**
	 * Check name and aliases of the given bean for URLs,
	 * detected by starting with "/".
	 * @param beanName the name of the candidate bean
	 * @return the URLs determined for the bean, or an empty array if none
	 */
	protected String[] determineUrlsForHandler(String beanName) {
		List urls = new ArrayList();
		if (beanName.startsWith("/")) {
			urls.add(beanName);
		}
		String[] aliases = getApplicationContext().getAliases(beanName);
		for (int j = 0; j < aliases.length; j++) {
			if (aliases[j].startsWith("/")) {
				urls.add(aliases[j]);
			}
		}
		return StringUtils.toStringArray(urls);
	}

}
