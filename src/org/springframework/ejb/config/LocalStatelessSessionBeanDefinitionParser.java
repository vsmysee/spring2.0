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

package org.springframework.ejb.config;

import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;
import org.springframework.ejb.config.AbstractJndiLocatedBeanDefinitionParser;

import org.w3c.dom.Element;

/**
 * {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementation for parsing
 * '<code>local-slsb</code>' tags and creating {@link org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean} definitions.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean
 */
class LocalStatelessSessionBeanDefinitionParser extends AbstractJndiLocatedBeanDefinitionParser {

	protected Class getBeanClass(Element element) {
		return LocalStatelessSessionProxyFactoryBean.class;
	}

}
