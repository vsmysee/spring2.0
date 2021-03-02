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

package org.springframework.aop.config;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.StringUtils;

/**
 * {@link BeanDefinitionParser} implementation that for the '<code>aspectj-autoproxy</code>' tag
 * that enables the automatic application of @AspectJ-style aspects found in the
 * {@link org.springframework.beans.factory.BeanFactory}.
 *
 * @author Rob Harrop
 * @since 2.0
 */
class AspectJAutoProxyBeanDefinitionParser implements BeanDefinitionParser {

	private static final String PROXY_TARGET_ATTRIBUTE = "proxy-target-class";

	private static final String PROXY_TARGET_CLASS = Conventions.attributeNameToPropertyName(PROXY_TARGET_ATTRIBUTE);


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		AopNamespaceUtils.registerAtAspectJAutoProxyCreatorIfNecessary(parserContext, element);
		extendBeanDefinition(registry, element);
		return null;
	}

	private void extendBeanDefinition(BeanDefinitionRegistry registry, Element element) {
		BeanDefinition beanDef = registry.getBeanDefinition(AopNamespaceUtils.AUTO_PROXY_CREATOR_BEAN_NAME);
		String proxyTargetClass = element.getAttribute(PROXY_TARGET_ATTRIBUTE);
		if (StringUtils.hasText(proxyTargetClass)) {
			beanDef.getPropertyValues().addPropertyValue(PROXY_TARGET_CLASS, proxyTargetClass);
		}
		if (element.hasChildNodes()) {
			addIncludePatterns(element, beanDef);
		}
	}

	private void addIncludePatterns(Element element, BeanDefinition beanDef) {
		List includePatterns = new LinkedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element include = (Element) node;
				String patternText = include.getAttribute("name");
				includePatterns.add(patternText);
			}
		}
		beanDef.getPropertyValues().addPropertyValue("includePatterns", includePatterns);
	}

}
