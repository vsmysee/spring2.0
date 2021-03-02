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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * Bean definition reader for XML bean definitions. Delegates the actual XML
 * document reading to an implementation of the BeanDefinitionDocumentReader interface.
 *
 * <p>Typically applied to a DefaultListableBeanFactory or GenericApplicationContext.
 *
 * <p>This class loads a DOM document and applies the BeanDefinitionDocumentReader to it.
 * The document reader will register each bean definition with the given bean factory,
 * relying on the latter's implementation of the BeanDefinitionRegistry interface.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 26.11.2003
 * @see #setDocumentReaderClass
 * @see org.springframework.beans.factory.xml.BeanDefinitionDocumentReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * Indicates that the validation should be disabled.
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * Indicates that the validation mode should be detected automatically.
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * Indicates that DTD validation should be used.
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;
	/**
	 * Indicates that XSD validation should be used.
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


	/**
	 * {@link Constants} instance for this class.
	 */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	/**
	 * Are namespaces important?
	 */
	private boolean namespaceAware;

	/**
	 * The current validation mode. Defaults to {@link #VALIDATION_AUTO}.
	 */
	private int validationMode = VALIDATION_AUTO;

	/**
	 * The {@link org.xml.sax.ErrorHandler} to use when XML parsing errors occur.
	 */
	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	/**
	 * The {@link org.xml.sax.EntityResolver} implementation to use.
	 */
	private EntityResolver entityResolver;

	private Class parserClass;

	/**
	 * The {@link org.springframework.beans.factory.xml.BeanDefinitionDocumentReader} <code>Class</code> to use for reading.
	 */
	private Class documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

	/**
	 * The {@link org.springframework.beans.factory.parsing.ProblemReporter} used to report any errors or warnings during parsing.
	 */
	private ProblemReporter problemReporter = new FailFastProblemReporter();

	/**
	 * The {@link org.springframework.beans.factory.parsing.ReaderEventListener} that all component registration events should be sent to.
	 */
	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	/**
	 * The {@link org.springframework.beans.factory.parsing.SourceExtractor} to use when extracting
	 * {@link org.springframework.beans.factory.config.BeanDefinition#getSource() source objects}
	 * from the configuration data.
	 */
	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	/**
	 * The {@link org.springframework.beans.factory.xml.NamespaceHandlerResolver} implementation passed to the {@link org.springframework.beans.factory.xml.BeanDefinitionDocumentReader}.
	 */
	private NamespaceHandlerResolver namespaceHandlerResolver;

	/**
	 * The {@link org.springframework.beans.factory.xml.DocumentLoader} to use for loading DOM {@link org.w3c.dom.Document Documents}.
	 */
	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	/**
	 * The helper object to use for autodetection of validation mode
	 */
	private XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();


	/**
	 * Create new XmlBeanDefinitionReader for the given bean factory.
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry beanFactory) {
		super(beanFactory);

		// Determine EntityResolver to use.
		if (getResourceLoader() != null) {
			this.entityResolver = new ResourceEntityResolver(getResourceLoader());
		}
		else {
			this.entityResolver = new DelegatingEntityResolver(ClassUtils.getDefaultClassLoader());
		}
	}


	/**
	 * Set whether or not the XML parser should be XML namespace aware.
	 * Default is "false".
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * Set if the XML parser should validate the document and thus enforce a DTD.
	 * @deprecated as of Spring 2.0: superseded by "validationMode"
	 * @see #setValidationMode
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
	}

	/**
	 * Set the validation mode to use by name. Defaults to {@link #VALIDATION_AUTO}.
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * Set the validation mode to use. Defaults to {@link #VALIDATION_AUTO}.
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * Specify which {@link org.springframework.beans.factory.parsing.ProblemReporter} to use. Default implementation is
	 * {@link org.springframework.beans.factory.parsing.FailFastProblemReporter} which exhibits fail fast behaviour. External tools
	 * can provide an alternative implementation that collates errors and warnings for
	 * display in the tool UI.
	 */
	public void setProblemReporter(ProblemReporter problemReporter) {
		Assert.notNull(problemReporter, "'problemReporter' cannot be null.");
		this.problemReporter = problemReporter;
	}

	/**
	 * Specify which {@link org.springframework.beans.factory.parsing.ReaderEventListener} to use. Default implementation is
	 * EmptyReaderEventListener which discards every event notification. External tools
	 * can provide an alternative implementation to monitor the components being registered
	 * in the BeanFactory.
	 */
	public void setEventListener(ReaderEventListener eventListener) {
		Assert.notNull(eventListener, "'eventListener' cannot be null.");
		this.eventListener = eventListener;
	}

	/**
	 * Specify the {@link org.springframework.beans.factory.parsing.SourceExtractor} to use. The default implementation is
	 * {@link org.springframework.beans.factory.parsing.NullSourceExtractor} which simply returns <code>null</code> as the source object.
	 * This means that during normal runtime execution no additional source metadata is attached
	 * to the bean configuration metadata.
	 */
	public void setSourceExtractor(SourceExtractor sourceExtractor) {
		Assert.notNull(sourceExtractor, "'sourceExtractor' cannot be null.");
		this.sourceExtractor = sourceExtractor;
	}

	/**
	 * Specify the {@link org.springframework.beans.factory.xml.NamespaceHandlerResolver} to use. If none is specified a default
	 * instance will be created by {@link #createDefaultNamespaceHandlerResolver()}.
	 */
	public void setNamespaceHandlerResolver(NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * Gets the current {@link org.springframework.beans.factory.xml.NamespaceHandlerResolver}. Creates the default implementation
	 * if necessary.
	 * @see #createDefaultNamespaceHandlerResolver() 
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if(this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * Specify the {@link org.springframework.beans.factory.xml.DocumentLoader} to use. The default implementation is
	 * {@link org.springframework.beans.factory.xml.DefaultDocumentLoader} which loads {@link org.w3c.dom.Document} instances using JAXP.
	 */
	public void setDocumentLoader(DocumentLoader documentLoader) {
		Assert.notNull(documentLoader, "'documentLoader' cannot be null.");
		this.documentLoader = documentLoader;
	}

	/**
	 * Set an implementation of the <code>org.xml.sax.ErrorHandler</code>
	 * interface for custom handling of XML parsing errors and warnings.
	 * <p>If not set, a default SimpleSaxErrorHandler is used that simply
	 * logs warnings using the logger instance of the view class,
	 * and rethrows errors to discontinue the XML transformation.
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Set a SAX entity resolver to be used for parsing. By default,
	 * BeansDtdResolver will be used. Can be overridden for custom entity
	 * resolution, for example relative to some specific base path.
	 * @see org.springframework.beans.factory.xml.BeansDtdResolver
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}


	/**
	 * Gets the SAX {@link org.xml.sax.EntityResolver}.
	 */
	public EntityResolver getEntityResolver() {
		return entityResolver;
	}

	/**
	 * Set the XmlBeanDefinitionParser implementation to use,
	 * responsible for the actual parsing of XML bean definitions.
	 * @deprecated as of Spring 2.0: superseded by "documentReaderClass"
	 * @see #setDocumentReaderClass
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionParser
	 */
	public void setParserClass(Class parserClass) {
		if (this.parserClass == null || !XmlBeanDefinitionParser.class.isAssignableFrom(parserClass)) {
			throw new IllegalArgumentException("parserClass must be an XmlBeanDefinitionParser");
		}
		this.parserClass = parserClass;
	}

	/**
	 * Specify the BeanDefinitionDocumentReader implementation to use,
	 * responsible for the actual reading of the XML bean definition document.
	 * <p>Default is DefaultBeanDefinitionDocumentReader.
	 * @param documentReaderClass the desired BeanDefinitionDocumentReader implementation class
	 * @see org.springframework.beans.factory.xml.BeanDefinitionDocumentReader
	 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
	 */
	public void setDocumentReaderClass(Class documentReaderClass) {
		if (documentReaderClass == null || !BeanDefinitionDocumentReader.class.isAssignableFrom(documentReaderClass)) {
			throw new IllegalArgumentException(
					"documentReaderClass must be an implementation of the BeanDefinitionDocumentReader interface");
		}
		this.documentReaderClass = documentReaderClass;
	}


	/**
	 * Load bean definitions from the specified XML file.
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param encodedResource the resource descriptor for the XML file,
	 * allowing to specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isInfoEnabled()) {
			logger.info("Loading XML bean definitions from " + encodedResource.getResource());
		}

		try {
			InputStream inputStream = encodedResource.getResource().getInputStream();
			try {
				InputSource inputSource = new InputSource(inputStream);
				if (encodedResource.getEncoding() != null) {
					inputSource.setEncoding(encodedResource.getEncoding());
				}
				return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * Load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @param resourceDescription a description of the resource
	 * (can be <code>null</code> or empty)
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource, String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * Actually load bean definitions from the specified XML file.
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of loading or parsing errors
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {
		try {
			int validationMode = getValidationModeForResource(resource);
			Document doc = this.documentLoader.loadDocument(
					inputSource, this.entityResolver, this.errorHandler, validationMode, this.namespaceAware);
			return registerBeanDefinitions(doc, resource);
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (SAXParseException ex) {
			throw new BeanDefinitionStoreException(
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new BeanDefinitionStoreException("XML document from " + resource + " is invalid", ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException("Unexpected exception parsing XML document from " + resource, ex);
		}
	}


	/**
	 * Gets the validation mode for the specified {@link Resource}. If no explicit
	 * validation mode has been configured then the validation mode is
	 * {@link #detectValidationMode detected}.
	 */
	private int getValidationModeForResource(Resource resource) {
		return (this.validationMode != VALIDATION_AUTO ? this.validationMode : detectValidationMode(resource));
	}

	/**
	 * Detects which kind of validation to perform on the XML file identified
	 * by the supplied {@link Resource}. If the
	 * file has a <code>DOCTYPE</code> definition then DTD validation is used
	 * otherwise XSD validation is assumed.
	 */
	protected int detectValidationMode(Resource resource) {
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
							"Passed-in Resource [" + resource + "] contains an open stream: " +
											"cannot determine validation mode automatically. Either pass in a Resource " +
											"that is able to create fresh streams, or explicitly specify the validationMode " +
											"on your XmlBeanDefinitionReader instance.");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
							"Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
											"Did you attempt to load directly from a SAX InputSource without specifying the " +
											"validationMode on your XmlBeanDefinitionReader instance?", ex);
		}

		try {
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
							resource + "]: an error occurred whilst reading from the InputStream.", ex);
		}
	}

	/**
	 * Register the bean definitions contained in the given DOM document.
	 * Called by <code>loadBeanDefinitions</code>.
	 * <p>Creates a new instance of the parser class and invokes
	 * <code>registerBeanDefinitions</code> on it.
	 * @param doc the DOM document
	 * @param resource the resource descriptor (for context information)
	 * @return the number of bean definitions found
	 * @throws org.springframework.beans.factory.BeanDefinitionStoreException in case of parsing errors
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see org.springframework.beans.factory.xml.BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		// Support old XmlBeanDefinitionParser SPI for backwards-compatibility.
		if (this.parserClass != null) {
			XmlBeanDefinitionParser parser =
					(XmlBeanDefinitionParser) BeanUtils.instantiateClass(this.parserClass);
			return parser.registerBeanDefinitions(this, doc, resource);
		}
		// Read document based on new BeanDefinitionDocumentReader SPI.
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		int countBefore = getBeanFactory().getBeanDefinitionCount();
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getBeanFactory().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * Create the {@link org.springframework.beans.factory.xml.BeanDefinitionDocumentReader} to use for actually
	 * reading bean definitions from an XML document.
	 * <p>Default implementation instantiates the specified "documentReaderClass".
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return (BeanDefinitionDocumentReader) BeanUtils.instantiateClass(this.documentReaderClass);
	}

	/**
	 * Create the {@link XmlReaderContext} to pass over to the document reader.
	 */
	protected XmlReaderContext createReaderContext(Resource resource) {
		NamespaceHandlerResolver resolver = this.namespaceHandlerResolver;
		if (resolver == null) {
			resolver = createDefaultNamespaceHandlerResolver();
		}
		return new XmlReaderContext(
				resource, this.problemReporter, this.eventListener, this.sourceExtractor, this, resolver);
	}

	/**
	 * Create the default implementation of {@link org.springframework.beans.factory.xml.NamespaceHandlerResolver} used if none is specified.
	 * Default implementation returns an instance of {@link org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver}.
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		return new DefaultNamespaceHandlerResolver(getResourceLoader().getClassLoader());
	}

}
