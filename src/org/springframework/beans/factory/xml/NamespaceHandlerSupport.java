/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Support class for implementing custom {@link org.springframework.beans.factory.xml.NamespaceHandler NamespaceHandlers}. Parsing and
 * decorating of individual {@link org.w3c.dom.Node Nodes} is done via {@link org.springframework.beans.factory.xml.BeanDefinitionParser} and
 * {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} strategy interfaces respectively. Provides the
 * {@link #registerBeanDefinitionParser}, {@link #registerBeanDefinitionDecorator} methods
 * for registering a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} or {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} to handle
 * a specific element.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see #registerBeanDefinitionParser(String, org.springframework.beans.factory.xml.BeanDefinitionParser)
 * @see #registerBeanDefinitionDecorator(String, org.springframework.beans.factory.xml.BeanDefinitionDecorator)
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

	/**
	 * Stores the {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementations keyed by the
	 * local name of the {@link org.w3c.dom.Element Elements} they handle.
	 */
	private final Map parsers = new HashMap();

	/**
	 * Stores the {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} implementations keyed by the
	 * local name of the {@link org.w3c.dom.Element Elements} they handle.
	 */
	private final Map decorators = new HashMap();

	/**
	 * Stores the {@link org.springframework.beans.factory.xml.BeanDefinitionParser} implementations keyed by the local
	 * name of the {@link org.w3c.dom.Attr Attrs} they handle.
	 */
	private final Map attributeDecorators = new HashMap();

	/**
	 * Decorates the supplied {@link org.w3c.dom.Node} by delegating to the {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} that
	 * is registered to handle that {@link org.w3c.dom.Node}.
	 */
	public final BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
		return findDecoratorForNode(node).decorate(node, definition, parserContext);
	}

	/**
	 * Parses the supplied {@link org.w3c.dom.Element} by delegating to the {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that is
	 * registered for that {@link org.w3c.dom.Element}.
	 */
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		return findParserForElement(element).parse(element, parserContext);
	}

	/**
	 * Locates the {@link org.springframework.beans.factory.xml.BeanDefinitionParser} from the register implementations using
	 * the local name of the supplied {@link org.w3c.dom.Element}.
	 */
	protected final BeanDefinitionParser findParserForElement(Element element) {
		BeanDefinitionParser parser = (BeanDefinitionParser) this.parsers.get(element.getLocalName());

		if (parser == null) {
			throw new IllegalArgumentException("Cannot locate BeanDefinitionParser for element [" +
							element.getLocalName() + "].");
		}

		return parser;
	}

	/**
	 * Locates the {@link org.springframework.beans.factory.xml.BeanDefinitionParser} from the register implementations using
	 * the local name of the supplied {@link org.w3c.dom.Node}. Supports both {@link org.w3c.dom.Element Elements}
	 * and {@link org.w3c.dom.Attr Attrs}.
	 */
	protected final BeanDefinitionDecorator findDecoratorForNode(Node node) {
		BeanDefinitionDecorator decorator = null;
		if (node instanceof Element) {
			decorator = (BeanDefinitionDecorator) this.decorators.get(node.getLocalName());
		}
		else if (node instanceof Attr) {
			decorator = (BeanDefinitionDecorator) this.attributeDecorators.get(node.getLocalName());
		}
		else {
			throw new IllegalArgumentException("Cannot decorate based on Nodes of type '" + node.getClass().getName() + "'");
		}

		if (decorator == null) {
			throw new IllegalArgumentException("Cannot locate BeanDefinitionDecorator for "
							+ (node instanceof Element ? "element" : "attribute") + " [" +
							node.getLocalName() + "].");
		}

		return decorator;
	}

	/**
	 * Subclasses can call this to register the supplied {@link org.springframework.beans.factory.xml.BeanDefinitionParser} to
	 * handle the specified element. The element name is the local (non-namespace qualified)
	 * name.
	 */
	protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
		this.parsers.put(elementName, parser);
	}

	/**
	 * Subclasses can call this to register the supplied {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} to
	 * handle the specified element. The element name is the local (non-namespace qualified)
	 * name.
	 */
	protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator decorator) {
		this.decorators.put(elementName, decorator);
	}

	/**
	 * Subclasses can call this to register the supplied {@link org.springframework.beans.factory.xml.BeanDefinitionDecorator} to
	 * handle the specified attribute. The attribute name is the local (non-namespace qualified)
	 * name.
	 */
	protected final void registerBeanDefinitionDecoratorForAttribute(String attributeName, BeanDefinitionDecorator decorator) {
		this.attributeDecorators.put(attributeName, decorator);
	}
}
