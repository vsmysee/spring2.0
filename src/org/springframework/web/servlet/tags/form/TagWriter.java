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

import org.springframework.util.StringUtils;
import org.springframework.util.Assert;

import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.io.Writer;
import java.util.Stack;

/**
 * Utility class for writing HTML content to a {@link java.io.Writer} instance.
 * 
 * <p>Intended to support output from JSP tag libraries.
 * 
 * @author Rob Harrop
 * @since 2.0
 */
public class TagWriter {

	/**
	 * The {@link org.springframework.web.servlet.tags.form.TagWriter.SafeWriter} to write to.
	 */
	private final SafeWriter writer;


	/**
	 * Stores {@link org.springframework.web.servlet.tags.form.TagWriter.TagStateEntry tag state}. Stack model naturally supports tag nesting.
	 */
	private Stack tagState = new Stack();


	/**
	 * Creates a new instance of the {@link org.springframework.web.servlet.tags.form.TagWriter} class that writes to
	 * the supplied {@link java.io.Writer}.
	 * @param writer the {@link java.io.Writer} to write tag content to
	 * @throws IllegalArgumentException oif the supplied {@link java.io.Writer} is <code>null</code>
	 */
	public TagWriter(Writer writer) {
		Assert.notNull(writer);
		this.writer = new SafeWriter(writer);
	}


	/**
	 * Starts a new tag with the supplied name. Leaves the tag open so
	 * that attributes, inner text or nested tags can be written into it.
	 * @see #endTag()
	 */
	public void startTag(String tagName) throws JspException {
		if (inTag()) {
			closeTagAndMarkAsBlock();
		}
		push(tagName);
		this.writer.append("<").append(tagName);
	}

	/**
	 * Writes an HTML attribute with the specified name and value.
	 * <p>Be sure to write all attributes <strong>before</strong> writing
	 * any inner text or nested tags.
	 * @throws IllegalStateException if the opening tag is closed
	 */
	public void writeAttribute(String attributeName, String attributeValue) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName).append("=\"")
				.append(attributeValue).append("\"");
	}

	/**
	 * Writes an HTML attribute if the supplied value is not <code>null</code>
	 * or zero length.
	 * @see #writeAttribute(String, String)
	 */
	public void writeOptionalAttributeValue(String attributeName, String attributeValue) throws JspException {
		if (StringUtils.hasText(attributeValue)) {
			writeAttribute(attributeName, attributeValue);
		}
	}

	/**
	 * Closes the current opening tag (if necessary) and appends the
	 * supplied value as inner text.
	 * @throws IllegalStateException if no tag is open
	 */
	public void appendValue(String value) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write tag value. No open tag available.");
		}
		closeTagAndMarkAsBlock();
		this.writer.append(value);
	}


	/**
	 * Indicates that the currently open tag should be closed and marked
	 * as a block level element.
	 * <p>Useful when you plan to write additional content in the body
	 * outside the context of the current {@link org.springframework.web.servlet.tags.form.TagWriter}.
	 */
	public void forceBlock() throws JspException {
		if (currentState().isBlockTag()) {
			return; // just ignore since we are already in the block
		}
		closeTagAndMarkAsBlock();
	}

	/**
	 * Closes the current tag.
	 * <p>Correctly writes an empty tag if no inner text or nested tags
	 * have been written.
	 */
	public void endTag() throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write end of tag. No open tag available.");
		}
		if (currentState().isBlockTag()) {
			// writing the end of the block - the opening tag was closed earlier.
			this.writer.append("</").append(currentState().getTagName()).append(">");
		}
		else {
			this.writer.append("/>");
		}
		this.tagState.pop();
	}


	/**
	 * Adds the supplied tag name to the {@link #tagState tag state}.
	 */
	private void push(String tagName) {
		this.tagState.push(new TagStateEntry(tagName));
	}

	/**
	 * Closes the current opening tag and marks it as a block tag.
	 */
	private void closeTagAndMarkAsBlock() throws JspException {
		if (!currentState().isBlockTag()) {
			currentState().markAsBlockTag();
			this.writer.append(">");
		}
	}

	private boolean inTag() {
		return this.tagState.size() > 0;
	}

	private TagStateEntry currentState() {
		return (TagStateEntry) this.tagState.peek();
	}


	/**
	 * Holds state about a tag and its rendered behaviour.
	 */
	private static final class TagStateEntry {

		private final String tagName;

		private boolean blockTag;


		public TagStateEntry(String tagName) {
			this.tagName = tagName;
		}


		public String getTagName() {
			return tagName;
		}

		public boolean isBlockTag() {
			return blockTag;
		}


		public void markAsBlockTag() {
			this.blockTag = true;
		}

	}


	/**
	 * Simple {@link java.io.Writer} wrapper that wraps all
	 * {@link java.io.IOException IOExceptions} in {@link javax.servlet.jsp.JspException JspExceptions}.
	 */
	private static final class SafeWriter {

		private final Writer writer;

		
		public SafeWriter(Writer writer) {
			this.writer = writer;
		}

		
		public SafeWriter append(String value) throws JspException {
			try {
				this.writer.write(String.valueOf(value));
				return this;
			}
			catch (IOException ex) {
				throw new JspException("Unable to write to JspWriter", ex);
			}
		}
		
	}

}
