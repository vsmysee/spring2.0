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

package org.springframework.web.servlet.tags.form;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.TestBean;
import org.springframework.beans.Colour;
import org.springframework.beans.Pet;

import javax.servlet.jsp.tagext.Tag;
import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class CheckboxTagTests extends AbstractFormTagTests {

	private CheckboxTag tag;

	private TestBean bean;

	protected void onSetUp() {
		this.tag = new CheckboxTag() {
			protected TagWriter createTagWriter() {
				return new TagWriter(getWriter());
			}
		};
		this.tag.setPageContext(getPageContext());
	}

	public void testWithSingleValueBooleanObjectChecked() throws Exception {
		this.tag.setPath("someBoolean");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);
		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("Both tag and hidden element not rendered", 2, rootElement.elements().size());
		Element checkboxElement = (Element) rootElement.elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("someBoolean", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("true", checkboxElement.attribute("value").getValue());
	}

	public void testWithSingleValueBooleanChecked() throws Exception {
		this.tag.setPath("jedi");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);
		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("jedi", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("true", checkboxElement.attribute("value").getValue());
	}

	public void testWithSingleValueBooleanObjectUnchecked() throws Exception {
		this.bean.setSomeBoolean(new Boolean(false));
		this.tag.setPath("someBoolean");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("someBoolean", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
		assertEquals("true", checkboxElement.attribute("value").getValue());
	}

	public void testWithSingleValueBooleanUnchecked() throws Exception {
		this.bean.setJedi(false);
		this.tag.setPath("jedi");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("jedi", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
		assertEquals("true", checkboxElement.attribute("value").getValue());
	}

	public void testWithSingleValueNull() throws Exception {
		this.bean.setName(null);
		this.tag.setPath("name");
		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("name", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
		assertEquals("Rob Harrop", checkboxElement.attribute("value").getValue());
	}

	public void testWithSingleValueNotNull() throws Exception {
		this.bean.setName("Rob Harrop");
		this.tag.setPath("name");
		this.tag.setValue("Rob Harrop");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("name", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("Rob Harrop", checkboxElement.attribute("value").getValue());
	}

	public void testWithMultiValueChecked() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("foo", checkboxElement.attribute("value").getValue());
	}

	public void testWithMultiValueUnchecked() throws Exception {
		this.tag.setPath("stringArray");
		this.tag.setValue("abc");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("stringArray", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
		assertEquals("abc", checkboxElement.attribute("value").getValue());
	}

	public void testWithCollection() throws Exception {
		this.tag.setPath("someList");
		this.tag.setValue("foo");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("someList", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("foo", checkboxElement.attribute("value").getValue());
	}

	public void testWithObjectChecked() throws Exception {
		this.tag.setPath("date");
		this.tag.setValue(getDate());

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("date", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals(getDate().toString(), checkboxElement.attribute("value").getValue());
	}

	public void testWithObjectUnchecked() throws Exception {
		this.tag.setPath("date");
		Date date = new Date();
		this.tag.setValue(date);

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("date", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
		assertEquals(date.toString(), checkboxElement.attribute("value").getValue());
	}

	public void testCollectionOfColoursSelected() throws Exception {
		this.tag.setPath("otherColours");
		this.tag.setValue("RED");

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("otherColours", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
	}

	public void testCollectionOfColoursNotSelected() throws Exception {
		this.tag.setPath("otherColours");
		this.tag.setValue("PURPLE");

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("otherColours", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
	}

	public void testCollectionOfPetsAsString() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue("Spot");

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("pets", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
	}

	public void testCollectionOfPetsAsStringNotSelected() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue("Santa's Little Helper");

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("pets", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
	}

	public void testCollectionOfPets() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue(new Pet("Rudiger"));

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("pets", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
	}

	public void testCollectionOfPetsNotSelected() throws Exception {
		this.tag.setPath("pets");
		this.tag.setValue(new Pet("Santa's Little Helper"));

		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);

		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element checkboxElement = (Element) document.getRootElement().elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("pets", checkboxElement.attribute("name").getValue());
		assertNull(checkboxElement.attribute("checked"));
	}

	public void testWithNullValue() throws Exception {
		try {
			this.tag.setPath("name");
			this.tag.doStartTag();
			fail("Should not be able to render with a null value when binding to a non-boolean.");
		}
		catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testHiddenElementOmittedOnDisabled() throws Exception {
		this.tag.setPath("someBoolean");
		this.tag.setDisabled("true");
		int result = this.tag.doStartTag();
		assertEquals(Tag.EVAL_PAGE, result);
		String output = getWriter().toString();

		// wrap the output so it is valid XML
		output = "<doc>" + output + "</doc>";

		SAXReader reader = new SAXReader();
		Document document = reader.read(new StringReader(output));
		Element rootElement = document.getRootElement();
		assertEquals("Both tag and hidden element rendered incorrectly", 1, rootElement.elements().size());
		Element checkboxElement = (Element) rootElement.elements().get(0);
		assertEquals("input", checkboxElement.getName());
		assertEquals("checkbox", checkboxElement.attribute("type").getValue());
		assertEquals("someBoolean", checkboxElement.attribute("name").getValue());
		assertEquals("checked", checkboxElement.attribute("checked").getValue());
		assertEquals("true", checkboxElement.attribute("value").getValue());
	}
	private Date getDate() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, 10);
		cal.set(Calendar.MONTH, 10);
		cal.set(Calendar.DATE, 10);
		cal.set(Calendar.HOUR, 10);
		cal.set(Calendar.MINUTE, 10);
		cal.set(Calendar.SECOND, 10);
		return cal.getTime();
	}

	protected TestBean createTestBean() {
		List colours = new ArrayList();
		colours.add(Colour.BLUE);
		colours.add(Colour.RED);
		colours.add(Colour.GREEN);

		List pets = new ArrayList();
		pets.add(new Pet("Rudiger"));
		pets.add(new Pet("Spot"));
		pets.add(new Pet("Fluffy"));
		pets.add(new Pet("Mufty"));

		this.bean = new TestBean();
		this.bean.setDate(getDate());
		this.bean.setName("Rob Harrop");
		this.bean.setJedi(true);
		this.bean.setSomeBoolean(new Boolean(true));
		this.bean.setStringArray(new String[]{"foo", "bar"});
		this.bean.setOtherColours(colours);
		this.bean.setPets(pets);
		List list = new ArrayList();
		list.add("foo");
		list.add("bar");
		this.bean.setSomeList(list);
		return this.bean;
	}


}
