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

package org.springframework.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class CollectionUtilsTests extends TestCase {

	public void testIsEmpty() {
		assertTrue(CollectionUtils.isEmpty((Set) null));
		assertTrue(CollectionUtils.isEmpty((Map) null));
		assertTrue(CollectionUtils.isEmpty(new HashMap()));
		assertTrue(CollectionUtils.isEmpty(new HashSet()));

		List list = new LinkedList();
		list.add(new Object());
		assertFalse(CollectionUtils.isEmpty(list));

		Map map = new HashMap();
		map.put("foo", "bar");
		assertFalse(CollectionUtils.isEmpty(map));
	}

	public void testContains() {
		assertFalse(CollectionUtils.contains((Iterator) null, "myElement"));
		assertFalse(CollectionUtils.contains((Enumeration) null, "myElement"));
		assertFalse(CollectionUtils.contains(new LinkedList().iterator(), "myElement"));
		assertFalse(CollectionUtils.contains(new Hashtable().keys(), "myElement"));

		List list = new LinkedList();
		list.add("myElement");
		assertTrue(CollectionUtils.contains(list.iterator(), "myElement"));

		Hashtable ht = new Hashtable();
		ht.put("myElement", "myValue");
		assertTrue(CollectionUtils.contains(ht.keys(), "myElement"));
	}

	public void testHasUniqueObject() {
		List list = new LinkedList();
		list.add("myElement");
		list.add("myOtherElement");
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		list.add("myElement");
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		list.add("myElement");
		list.add(null);
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		list.add(null);
		list.add("myElement");
		assertFalse(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		list.add(null);
		list.add(null);
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		list.add(null);
		assertTrue(CollectionUtils.hasUniqueObject(list));

		list = new LinkedList();
		assertFalse(CollectionUtils.hasUniqueObject(list));
	}

	public void testFindFirstMatch() throws Exception {
		List source = new ArrayList();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List candidates = new ArrayList();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertEquals("def", CollectionUtils.findFirstMatch(source, candidates));
	}

	public void testContainsAny() throws Exception {
		List source = new ArrayList();
		source.add("abc");
		source.add("def");
		source.add("ghi");

		List candidates = new ArrayList();
		candidates.add("xyz");
		candidates.add("def");
		candidates.add("abc");

		assertTrue(CollectionUtils.containsAny(source, candidates));
		candidates.remove("def");
		assertTrue(CollectionUtils.containsAny(source, candidates));
		candidates.remove("abc");
		assertFalse(CollectionUtils.containsAny(source, candidates));
	}
}
