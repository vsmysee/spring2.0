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

package org.springframework.web.servlet.view.xslt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.JdkVersion;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.AssertThrows;

/**
 * @author Rob Harrop
 */
public class XsltViewTests extends TestCase {

	private static final String HTML_OUTPUT = "org/springframework/web/servlet/view/xslt/products.xsl";

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	protected void setUp() throws Exception {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	public void testWithNoSource() throws Exception {
		final XsltView view = getXsltView(HTML_OUTPUT);
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				view.render(new HashMap(), request, response);
			}
		}.runTest();
	}

	public void testWithoutUrl() throws Exception {
		final XsltView view = new XsltView();
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				view.afterPropertiesSet();
			}
		}.runTest();
	}

	public void testSimpleTransformWithSource() throws Exception {
		Source source = new StreamSource(getProductDataResource().getInputStream());
		Map model = new HashMap();
		model.put("someKey", source);
		doTestWithModel(model);
	}

	public void testSimpleTransformWithDocument() throws Exception {
		org.w3c.dom.Document document = getDomDocument();
		Map model = new HashMap();
		model.put("someKey", document);
		doTestWithModel(model);
	}

	public void testSimpleTransformWithNode() throws Exception {
		org.w3c.dom.Document document = getDomDocument();
		Map model = new HashMap();
		model.put("someKey", document.getDocumentElement());
		doTestWithModel(model);
	}

	public void testSimpleTransformWithInputStream() throws Exception {
		Map model = new HashMap();
		model.put("someKey", getProductDataResource().getInputStream());
		doTestWithModel(model);
	}

	public void testSimpleTransformWithReader() throws Exception {
		Map model = new HashMap();
		model.put("someKey", new InputStreamReader(getProductDataResource().getInputStream()));
		doTestWithModel(model);
	}

	public void testSimpleTransformWithResource() throws Exception {
		Map model = new HashMap();
		model.put("someKey", getProductDataResource());
		doTestWithModel(model);
	}

	public void testWithSourceKey() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.setSourceKey("actualData");

		Map model = new HashMap();
		model.put("actualData", getProductDataResource());
		model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
	}

	public void testContentTypeCarriedFromTemplate() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);

		Source source = new StreamSource(getProductDataResource().getInputStream());
		Map model = new HashMap();
		model.put("someKey", source);

		view.render(model, this.request, this.response);
		assertEquals("text/html", this.response.getContentType());
	}

	public void testModelParametersCarriedAcross() throws Exception {
		Map model = new HashMap();
		model.put("someKey", getProductDataResource());
		model.put("title", "Product List");
		doTestWithModel(model);
		assertTrue(this.response.getContentAsString().indexOf("Product List") > -1);
	}

	public void testStaticAttributesCarriedAcross() throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.setSourceKey("actualData");
		view.addStaticAttribute("title", "Product List");

		Map model = new HashMap();
		model.put("actualData", getProductDataResource());
		model.put("otherData", new ClassPathResource("dummyData.xsl", getClass()));

		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
		assertTrue(this.response.getContentAsString().indexOf("Product List") > -1);

	}

	private org.w3c.dom.Document getDomDocument() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		org.w3c.dom.Document document = builder.parse(getProductDataResource().getInputStream());
		return document;
	}

	private void doTestWithModel(Map model) throws Exception {
		XsltView view = getXsltView(HTML_OUTPUT);
		view.render(model, this.request, this.response);
		assertHtmlOutput(this.response.getContentAsString());
	}

	private void assertHtmlOutput(String output) throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_15) {
			// TODO: find out why the SAXReader.read call fails on JDK 1.4 and 1.3
			return;
		}

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		List nodes = document.getRootElement().selectNodes("/html/body/table/tr");

		Element tr1 = (Element) nodes.get(0);
		assertRowElement(tr1, "1", "Whatsit", "12.99");
		Element tr2 = (Element) nodes.get(1);
		assertRowElement(tr2, "2", "Thingy", "13.99");
		Element tr3 = (Element) nodes.get(2);
		assertRowElement(tr3, "3", "Gizmo", "14.99");
		Element tr4 = (Element) nodes.get(3);
		assertRowElement(tr4, "4", "Cranktoggle", "11.99");
	}

	private void assertRowElement(Element elem, String id, String name, String price) {
		Element idElem = (Element) elem.elements().get(0);
		Element nameElem = (Element) elem.elements().get(1);
		Element priceElem = (Element) elem.elements().get(2);

		assertEquals("ID incorrect.", id, idElem.getText());
		assertEquals("Name incorrect.", name, nameElem.getText());
		assertEquals("Price incorrect.", price, priceElem.getText());
	}

	private XsltView getXsltView(String templatePath) {
		XsltView view = new XsltView();
		view.setUrl(templatePath);
		view.setApplicationContext(new StaticApplicationContext());
		view.initApplicationContext();
		return view;
	}

	private Resource getProductDataResource() {
		return new ClassPathResource("productData.xml", getClass());
	}
}
