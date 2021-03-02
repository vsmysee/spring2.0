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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * Abstract {@link BeanDefinitionParser} implementation providing a number
 * of convenience methods and a
 * {@link org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#parseInternal template method}
 * that subclasses must override to provide the actual parsing logic.
 *
 * <p>Use this {@link BeanDefinitionParser} implementation when you want
 * to parse some arbitrarily complex XML into one or more
 * {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}. If you just want to parse some
 * XML into a single <code>BeanDefinition</code>, you may wish to consider
 * the simpler convenience extensions of this class, namely
 * {@link AbstractSingleBeanDefinitionParser} and
 * {@link AbstractSimpleBeanDefinitionParser}.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @since 2.0
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * Constant for the id attribute.
	 */
	public static final String ID_ATTRIBUTE = "id";


	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		AbstractBeanDefinition definition = parseInternal(element, parserContext);
		String id = resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(id) && !parserContext.isNested()) {
			throw new IllegalArgumentException(
					"Id is required for element '" + element.getLocalName() + "' when used as a top-level tag");
		}
		BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id);
		registerBeanDefinition(holder, parserContext.getRegistry(), parserContext.isNested());
		if (shouldFireEvents()) {
			BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
			postProcessComponentDefinition(componentDefinition);
			parserContext.getReaderContext().fireComponentRegistered(componentDefinition);
		}
		return definition;
	}

	/**
	 * Resolve the ID for the supplied {@link org.springframework.beans.factory.config.BeanDefinition}. When using {@link #shouldGenerateId generation},
	 * a name is generated automatically, otherwise the ID is extracted from the "id" attribute.
	 */
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
		if (shouldGenerateId()) {
			return BeanDefinitionReaderUtils.generateBeanName(
					definition, parserContext.getRegistry(), parserContext.isNested());
		}
		else {
			return element.getAttribute(ID_ATTRIBUTE);
		}
	}

	/**
	 * Register the supplied {@link org.springframework.beans.factory.config.BeanDefinitionHolder bean} with the supplied
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry registry}.
	 * <p>Subclasses can override this method to control whether or not the supplied
	 * {@link org.springframework.beans.factory.config.BeanDefinitionHolder bean} is actually even registered, or to
	 * register even more beans.
	 * <p>The default implementation registers the supplied {@link org.springframework.beans.factory.config.BeanDefinitionHolder bean}
	 * with the supplied {@link org.springframework.beans.factory.support.BeanDefinitionRegistry registry} only if the <code>isNested</code>
	 * parameter is <code>false</code>, because one typically does not want inner beans
	 * to be registered as top level beans.
	 * @param bean the bean to be registered
	 * @param registry the registry that the bean is to be registered with 
	 * @param isNested <code>true</code> if the supplied {@link org.springframework.beans.factory.config.BeanDefinitionHolder bean}
	 * was created from a nested element
	 * @see org.springframework.beans.factory.support.BeanDefinitionReaderUtils#registerBeanDefinition(org.springframework.beans.factory.config.BeanDefinitionHolder, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	protected void registerBeanDefinition(BeanDefinitionHolder bean, BeanDefinitionRegistry registry, boolean isNested) {
		if (!isNested) {
			BeanDefinitionReaderUtils.registerBeanDefinition(bean, registry);
		}
	}


	/**
	 * Central template method to actually parse the supplied {@link org.w3c.dom.Element}
	 * into one or more {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}.
	 * @param element	the element that is to be parsed into one or more {@link org.springframework.beans.factory.config.BeanDefinition BeanDefinitions}
	 * @param parserContext the object encapsulating the current state of the parsing process;
	 * provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
	 * @return the primary {@link org.springframework.beans.factory.config.BeanDefinition} resulting from the parsing of the supplied {@link org.w3c.dom.Element}
	 * @see #parse(org.w3c.dom.Element, ParserContext)
	 * @see #postProcessComponentDefinition(org.springframework.beans.factory.parsing.BeanComponentDefinition)
	 */
	protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

	/**
	 * Should an ID be generated instead of read for the passed in {@link org.w3c.dom.Element}?
	 * Disabled by default; subclasses can override this to enable ID generation.
	 */
	protected boolean shouldGenerateId() {
		return false;
	}

	/**
	 * Controls whether this instance is to
	 * {@link org.springframework.beans.factory.parsing.ReaderContext#fireComponentRegistered(org.springframework.beans.factory.parsing.ComponentDefinition) fire an event}
	 * when a bean definition has been totally parsed?
	 * <p>Implementations must return <code>true</code> if they want an event
	 * will be fired when a bean definition has been totally parsed; returning
	 * <code>false</code> means that an event will not be fired.
	 * <p>This implementation returns <code>true</code> by default; that is, an event
	 * will be fired when a bean definition has been totally parsed.
	 * @return <code>true</code> if this instance is to
	 * {@link org.springframework.beans.factory.parsing.ReaderContext#fireComponentRegistered(org.springframework.beans.factory.parsing.ComponentDefinition) fire an event}
	 * when a bean definition has been totally parsed
	 */
	protected boolean shouldFireEvents() {
		return true;
	}

	/**
	 * Hook method called after the primary parsing of a
	 * {@link org.springframework.beans.factory.parsing.BeanComponentDefinition} but before the
	 * {@link org.springframework.beans.factory.parsing.BeanComponentDefinition} has been registered with a
	 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
	 * <p>Derived classes can override this emthod to supply any custom logic that
	 * is to be executed after all the parsing is finished.
	 * <p>The default implementation is a no-op.
	 * @param componentDefinition the {@link org.springframework.beans.factory.parsing.BeanComponentDefinition} that is to be processed
	 */
	protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
	}

}
