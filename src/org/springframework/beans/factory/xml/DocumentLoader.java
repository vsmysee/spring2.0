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

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

/**
 * Strategy interface for loading a {@link org.w3c.dom.Document}.
 * 
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.beans.factory.xml.DefaultDocumentLoader
 */
public interface DocumentLoader {

	/**
	 * Load a {@link org.w3c.dom.Document document} from the supplied {@link org.xml.sax.InputSource source}.
	 * @param inputSource the source of the document that is to be loaded
	 * @param entityResolver the resolver that is to be used to resolve any entities
	 * @param errorHandler used to report any errors during document loading
	 * @param validationMode the type of validation ({@link XmlBeanDefinitionReader#VALIDATION_NONE none}, {@link XmlBeanDefinitionReader#VALIDATION_DTD DTD}, or {@link XmlBeanDefinitionReader#VALIDATION_XSD XSD})
	 * @param namespaceAware <code>true</code> if the loading is provide support for XML namespaces 
	 * @return the loaded {@link org.w3c.dom.Document document}
	 * @throws Exception if an error occurs
	 */
	Document loadDocument(
            InputSource inputSource, EntityResolver entityResolver,
            ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
			throws Exception;

}
