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

package org.springframework.beans.factory.config;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * Simple {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor} implementation that effects the
 * registration of custom {@link Scope Scope(s)} in a {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}.
 * 
 * <p>Will register all of the supplied {@link #setScopes(java.util.Map) scopes}
 * with the {@link org.springframework.beans.factory.config.ConfigurableListableBeanFactory} that is passed to the
 * {@link #postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)} method.
 *
 * @author Rick Evans
 * @since 2.0
 */
public class CustomScopeConfigurer implements BeanFactoryPostProcessor, BeanClassLoaderAware, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered

	private Map scopes;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * Specify the custom scopes that are to be registered.
	 * <p>The keys indicate the scope names (of type String); each value
	 * is expected to be the corresponding custom {@link Scope} instance.
	 */
	public void setScopes(Map scopes) {
		this.scopes = scopes;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.scopes != null) {
			for (Iterator it = this.scopes.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				Object key = entry.getKey();
				if (!(key instanceof String)) {
					throw new IllegalArgumentException(
							"Invalid scope key [" + key + "]: only Strings allowed");
				}
				Object value = entry.getValue();
				if (!(value instanceof Scope)) {
					throw new IllegalArgumentException("Mapped value [" + value + "] for scope key [" +
							key + "] is not of required type [" + Scope.class.getName() + "]");
				}
				beanFactory.registerScope((String) key, (Scope) value);
			}
		}
	}

}
