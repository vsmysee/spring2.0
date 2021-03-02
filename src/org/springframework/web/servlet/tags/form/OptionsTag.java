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

import javax.servlet.jsp.JspException;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.util.TagUtils;

/**
 * Convenient tag that allows one to supply a collection of objects
 * that are to be rendered as '<code>option</code>' tags within a
 * '<code>select</code>' tag.
 * 
 * <p><i>Must</i> be used within a {@link SelectTag 'select' tag}.
 * 
 * @author Rob Harrop
 * @since 2.0
 */
public class OptionsTag extends AbstractFormTag {
	
	/**
	 * The {@link java.util.Collection}, {@link java.util.Map} or array of
	 * objects used to generate the inner '<code>option</code>' tags.
	 */
	private Object items;

	/**
	 * The name of the property mapped to the '<code>value</code>' attribute
	 * of the '<code>option</code>' tag.
	 */
	private String itemValue;

	/**
	 * The name of the property mapped to the inner text of the
	 * '<code>option</code>' tag.
	 */
	private String itemLabel;


	/**
	 * Sets the {@link java.util.Collection}, {@link java.util.Map} or array
	 * of objects used to generate the inner '<code>option</code>' tags.
	 * <p>Required when wishing to render '<code>option</code>' tags from an
	 * array, {@link java.util.Collection} or {@link java.util.Map}.
	 * <p>Typically a runtime expression.
	 * @param items said items
	 * @throws IllegalArgumentException if the supplied <code>items</code> instance is <code>null</code> 
	 */
	public void setItems(Object items) {
		Assert.notNull(items, "'items' cannot be null.");
		this.items = items;
	}

	/**
	 * Gets the {@link java.util.Collection}, {@link java.util.Map} or array
	 * of objects used to generate the inner '<code>option</code>' tags.
	 * <p>Typically a runtime expression.
	 */
	protected Object getItems() {
		return this.items;
	}

	/**
	 * Sets the name of the property mapped to the '<code>value</code>'
	 * attribute of the '<code>option</code>' tag.
	 * <p>Required when wishing to render '<code>option</code>' tags from
	 * an array or {@link java.util.Collection}.
	 * <p>May be a runtime expression.
	 * @param itemValue said item value
	 * @throws IllegalArgumentException if the supplied <code>itemValue</code>
	 * is <code>null</code> or consists wholly of whitespace
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' cannot be null or zero length.");
		this.itemValue = itemValue;
	}

	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * Sets the name of the property mapped to the label (inner text) of the
	 * '<code>option</code>' tag.
	 * <p>May be a runtime expression.
	 * @param itemLabel said label
	 * @throws IllegalArgumentException if the supplied <code>itemLabel</code> is
	 * <code>null</code> or consists wholly of whitespace
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' cannot be null or zero length.");
		this.itemLabel = itemLabel;
	}

	/**
	 * Gets the name of the property mapped to the label (inner text) of the
	 * '<code>option</code>' tag.
	 * <p>May be a runtime expression.
	 */
	protected String getItemLabel() {
		return this.itemLabel;
	}


	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		assertUnderSelectTag();
		Object items = getItems();
		Object itemsObject = (items instanceof String ? evaluate("items", (String) items) : items);

		String itemValue = getItemValue();
		String itemLabel = getItemLabel();

		String valueProperty = (itemValue == null ? null :
						ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)));
		String labelProperty = (itemLabel == null ? null :
						ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)));

		OptionWriter optionWriter = new OptionWriter(itemsObject, getBindStatus(), valueProperty, labelProperty, isHtmlEscape());
		optionWriter.writeOptions(tagWriter);

		return EVAL_PAGE;
	}


	private void assertUnderSelectTag() {
		TagUtils.assertHasAncestorOfType(this, SelectTag.class, "options", "select");
	}

	private BindStatus getBindStatus() {
		return (BindStatus) this.pageContext.getAttribute(SelectTag.LIST_VALUE_PAGE_ATTRIBUTE);
	}

}
