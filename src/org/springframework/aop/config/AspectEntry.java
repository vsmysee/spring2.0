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

package org.springframework.aop.config;

import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.util.StringUtils;

/**
 * {@link ParseState} entry representing an aspect.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class AspectEntry implements ParseState.Entry {

	private final String id;

	private final String ref;


	/**
	 * Creates a new instance of the {@link org.springframework.aop.config.AspectEntry} class.
	 * @param id the id of the aspect element
	 * @param ref the bean name referenced by this aspect element
	 */
	public AspectEntry(String id, String ref) {
		this.id = id;
		this.ref = ref;
	}

	public String toString() {
		return "Aspect: " + (StringUtils.hasText(this.id) ? 
						"id='" + id + "'" : 
						"ref='" + ref + "'");
	}

}
