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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The default {@link DocumentLoader} implementation.
 * 
 * <p>Simply loads {@link org.w3c.dom.Document documents} using the standard JAXP-configured
 * XML parser. If you want to change the {@link javax.xml.parsers.DocumentBuilder} that is used to
 * load documents then one strategy is to use a Java define when starting your
 * application. For example, to use the Oracle {@link javax.xml.parsers.DocumentBuilder}, one might
 * start one's application like so:
 * 
 * <pre code="class">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 * 
 * @author Rob Harrop
 * @since 2.0
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * JAXP attribute used to configure the schema language for validation.
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * JAXP attribute value indicating the XSD schema language.
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * Load the {@link org.w3c.dom.Document} at the supplied {@link org.xml.sax.InputSource} using the standard JAXP-configured
	 * XML parser.
	 */
	public Document loadDocument(
			InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception {

		DocumentBuilderFactory factory =
						createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isDebugEnabled()) {
			logger.debug("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
	}


	/**
	 * Create the {@link javax.xml.parsers.DocumentBuilderFactory} instance.
	 * @param validationMode the type of validation ({@link XmlBeanDefinitionReader#VALIDATION_NONE none}, {@link XmlBeanDefinitionReader#VALIDATION_DTD DTD}, or {@link XmlBeanDefinitionReader#VALIDATION_XSD XSD})
	 * @param namespaceAware <code>true</code> if the returned factory is to provide support for XML namespaces
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
					throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlBeanDefinitionReader.VALIDATION_NONE) {
			factory.setValidating(true);

			if (validationMode == XmlBeanDefinitionReader.VALIDATION_XSD) {
				// enforce namespace aware for XSD
				factory.setNamespaceAware(true);
				try {
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					throw new BeanDefinitionStoreException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 or below with " +
							"Apache Crimson? Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
				}
			}
		}

		return factory;
	}

	/**
	 * Create a JAXP DocumentBuilder that this bean definition reader
	 * will use for parsing XML documents. Can be overridden in subclasses,
	 * adding further initialization of the builder.
	 * @param factory the JAXP DocumentBuilderFactory that the DocumentBuilder
	 * should be created with
	 * @return the JAXP DocumentBuilder
	 * @throws javax.xml.parsers.ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilder createDocumentBuilder(
			DocumentBuilderFactory factory, EntityResolver entityResolver, ErrorHandler errorHandler)
			throws ParserConfigurationException {

		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		return docBuilder;
	}

}
