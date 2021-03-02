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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;

/**
 * Implementation of the HandlerMapping interface to map from URLs to
 * request handler beans. Supports both mapping to bean instances and
 * mapping to bean names: The latter is required for prototype handlers.
 *
 * <p>The "urlMap" property is suitable for populating the handler map
 * with bean references, e.g. via the map element in XML bean definitions.
 *
 * <p>Mappings to bean names can be set via the "mappings" property, in a
 * form accepted by the java.util.Properties class, like as follows:<br>
 * <code>
 * /welcome.html=ticketController
 * /show.html=ticketController
 * </code><br>
 * The syntax is <code>PATH=HANDLER_BEAN_NAME</code>.
 * If the path doesn't begin with a slash, one is prepended.
 *
 * <p>Supports direct matches, e.g. a registered "/test" matches "/test",
 * and various Ant-style pattern matches, e.g. a registered "/t*" pattern
 * matches both "/test" and "/team", "/test/*" matches all paths in the
 * "/test" directory, "/test/**" matches all paths below "/test".
 * For syntax details, see the AntPathMatcher class.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.util.AntPathMatcher
 * @see java.util.Properties
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {
	
	private final Map urlMap = new HashMap();


	/**
	 * Map URL paths to handler bean names.
	 * This is the typical way of configuring this HandlerMapping.
	 * <p>Supports direct URL matches and Ant-style pattern matches.
	 * For syntax details, see the AntPathMatcher class.
	 * @param mappings properties with URLs as keys and bean names as values
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setMappings(Properties mappings) {
		this.urlMap.putAll(mappings);
	}

	/**
	 * Set a Map with URL paths as keys and handler beans as values.
	 * Convenient for population with bean references.
	 * <p>Supports direct URL matches and Ant-style pattern matches.
	 * For syntax details, see the AntPathMatcher class.
	 * @param urlMap map with URLs as keys and beans as values
	 * @see org.springframework.util.AntPathMatcher
	 */
	public void setUrlMap(Map urlMap) {
		this.urlMap.putAll(urlMap);
	}


	/**
	 * Calls the <code>registerHandlers</code> method in addition
	 * to the superclass's initialization.
	 * @see #registerHandlers
	 */
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * Register all handlers specified in the URL map for the corresponding paths.
	 * @param urlMap Map with URL paths as keys and handler beans or bean names as values
	 * @throws org.springframework.beans.BeansException if a handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandlers(Map urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.warn("Neither 'urlMap' nor 'mappings' set on SimpleUrlHandlerMapping");
		}
		else {
			Iterator it = urlMap.keySet().iterator();
			while (it.hasNext()) {
				String url = (String) it.next();
				Object handler = urlMap.get(url);
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				registerHandler(url, handler);
			}
		}
	}

}
