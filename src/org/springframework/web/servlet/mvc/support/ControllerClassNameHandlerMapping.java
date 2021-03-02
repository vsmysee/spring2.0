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

package org.springframework.web.servlet.mvc.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import org.springframework.web.servlet.mvc.throwaway.ThrowawayController;

/**
 * Implementation of {@link org.springframework.web.servlet.HandlerMapping} that follows a simple convention for
 * generating URL path mappings from the class names of registered
 * {@link org.springframework.web.servlet.mvc.Controller} and
 * {@link org.springframework.web.servlet.mvc.throwaway.ThrowawayController} beans.
 *
 * <p>For simple {@link org.springframework.web.servlet.mvc.Controller} implementations
 * (those that handle a single request type), the convention is to take the
 * {@link org.springframework.util.ClassUtils#getShortName short name} of the <code>Class</code>,
 * remove the 'Controller' suffix if it exists and return the remaining text, lowercased,
 * as the mapping, with a leading <code>/</code>. For example:
 * <ul>
 * <li><code>WelcomeController</code> -> <code>/welcome*</code></li>
 * <li><code>HomeController</code> -> <code>/home*</code></li>
 * </ul>
 *
 * <p>For {@link org.springframework.web.servlet.mvc.multiaction.MultiActionController MultiActionControllers} then a similar mapping is registered,
 * except that all sub-paths are registed using the trailing wildcard pattern <code>/*</code>.
 * For example:
 * <ul>
 * <li><code>WelcomeController</code> -> <code>/welcome/*</code></li>
 * <li><code>CatalogController</code> -> <code>/catalog/*</code></li>
 * </ul>
 *
 * <p>For {@link org.springframework.web.servlet.mvc.multiaction.MultiActionController} it is often useful to use
 * this mapping strategy in conjunction with the
 * {@link org.springframework.web.servlet.mvc.multiaction.InternalPathMethodNameResolver}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.servlet.mvc.throwaway.ThrowawayController
 * @see org.springframework.web.servlet.mvc.multiaction.MultiActionController
 */
public class ControllerClassNameHandlerMapping extends AbstractUrlHandlerMapping implements HandlerMapping {

	/**
	 * Common suffix at the end of controller implementation classes.
	 * Removed when generating the URL path.
	 */
	private static final String CONTROLLER_SUFFIX = "Controller";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * Calls the <code>detectControllers()</code> method in addition
	 * to the superclass's initialization.
	 * @see #detectControllers()
	 */
	protected void initApplicationContext() {
		super.initApplicationContext();
		detectControllers();
	}

	/**
	 * Detect all the {@link org.springframework.web.servlet.mvc.Controller} and
	 * {@link org.springframework.web.servlet.mvc.throwaway.ThrowawayController}
	 * beans registered in the {@link org.springframework.context.ApplicationContext}
	 * and register a URL path mapping for each one based on rules defined here.
	 * @throws org.springframework.beans.BeansException if the controllers couldn't be obtained or registered
	 * @see #generatePathMapping(Class)
	 */
	protected void detectControllers() throws BeansException {
		registerControllers(Controller.class);
		registerControllers(ThrowawayController.class);
	}

	/**
	 * Register all controllers of the given type, searching the current
	 * DispatcherServlet's ApplicationContext for matching beans.
	 * @param controllerType the type of controller to search for
	 * @throws org.springframework.beans.BeansException if the controllers couldn't be obtained or registered
	 */
	protected void registerControllers(Class controllerType) throws BeansException {
		String[] beanNames = getApplicationContext().getBeanNamesForType(controllerType);
		for (int i = 0; i < beanNames.length; i++) {
			registerController(beanNames[i]);
		}
	}

	/**
	 * Register the controller with the given name, as defined
	 * in the current application context.
	 * @param beanName the name of the controller bean
	 * @throws org.springframework.beans.BeansException if the controller couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 * @see #getApplicationContext()
	 */
	protected void registerController(String beanName) throws BeansException, IllegalStateException {
		Class controllerClass = getApplicationContext().getType(beanName);
		String urlPath = generatePathMapping(controllerClass);
		if (logger.isDebugEnabled()) {
			logger.debug("Registering Controller '" + beanName + "' as handler for URL path [" + urlPath + "]");
		}
		registerHandler(urlPath, beanName);
	}

	/**
	 * Generate the actual URL path for the given {@link org.springframework.web.servlet.mvc.Controller} class. Sub-classes
	 * may choose to customize the paths that are generated by overriding this method.
	 */
	protected String generatePathMapping(Class controllerClass) {
		StringBuffer pathMapping = new StringBuffer("/");
		String className = ClassUtils.getShortName(controllerClass.getName());
		String path = (className.endsWith(CONTROLLER_SUFFIX) ?
				className.substring(0, className.indexOf(CONTROLLER_SUFFIX)) : className);
		pathMapping.append(path.toLowerCase());
		if (MultiActionController.class.isAssignableFrom(controllerClass)) {
			pathMapping.append("/*");
		}
		else {
			pathMapping.append("*");
		}
		return pathMapping.toString();
	}

}
