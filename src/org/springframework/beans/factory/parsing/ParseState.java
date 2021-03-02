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

package org.springframework.beans.factory.parsing;

import java.util.Stack;

/**
 * Simple {@link java.util.Stack}-based structure for tracking the logical position during
 * a parsing process. {@link org.springframework.beans.factory.parsing.ParseState.Entry entries} are added to the stack at
 * each point during the parse phase in a reader-specific manner.
 *
 * <p>Calling {@link #toString()} will render a tree-style view of the current logical
 * position in the parse phase. This representation is intended for use in
 * error messages.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public final class ParseState {

	/**
	 * Tab character used when rendering the tree-style representation.
	 */
	private static final char TAB = '\t';

	/**
	 * Internal {@link java.util.Stack} storage.
	 */
	private final Stack state;


	/**
	 * Create a new <code>ParseState</code> with an empty {@link java.util.Stack}.
	 */
	public ParseState() {
		this.state = new Stack();
	}

	/**
	 * Create a new <code>ParseState</code> whose {@link java.util.Stack} is a {@link Object#clone clone}
	 * of that of the passed in <code>ParseState</code>.
	 */
	private ParseState(ParseState other) {
		this.state = (Stack) other.state.clone();
	}


	/**
	 * Add a new {@link org.springframework.beans.factory.parsing.ParseState.Entry} to the {@link java.util.Stack}.
	 */
	public void push(Entry entry) {
		this.state.push(entry);
	}

	/**
	 * Remove an {@link org.springframework.beans.factory.parsing.ParseState.Entry} from the {@link java.util.Stack}.
	 */
	public void pop() {
		this.state.pop();
	}

	/**
	 * Return the {@link org.springframework.beans.factory.parsing.ParseState.Entry} currently at the top of the {@link java.util.Stack} or
	 * <code>null</code> if the {@link java.util.Stack} is empty.
	 */
	public Entry peek() {
		return (Entry) (this.state.empty() ? null : this.state.peek());
	}

	/**
	 * Create a new instance of {@link org.springframework.beans.factory.parsing.ParseState} which is an independent snapshot
	 * of this instance.
	 */
	public ParseState snapshot() {
		return new ParseState(this);
	}


	/**
	 * Returns a tree-style representation of the current <code>ParseState</code>.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (int x = 0; x < this.state.size(); x++) {
			if (x > 0) {
				sb.append('\n');
				for (int y = 0; y < x; y++) {
					sb.append(TAB);
				}
				sb.append("-> ");
			}
			sb.append(this.state.get(x));
		}
		return sb.toString();
	}


	/**
	 * Marker interface for entries into the {@link org.springframework.beans.factory.parsing.ParseState}.
	 */
	public interface Entry {

	}

}
