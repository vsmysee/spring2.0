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

package org.springframework.validation;

import junit.framework.TestCase;
import org.springframework.beans.TestBean;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link org.springframework.validation.ValidationUtils} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 08.10.2004
 */
public final class ValidationUtilsTests extends TestCase {

	public void testInvokeValidatorWithNullValidator() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TestBean tb = new TestBean();
				Errors errors = new BindException(tb, "tb");
				ValidationUtils.invokeValidator(null, tb, errors);
			}
		}.runTest();
	}

	public void testInvokeValidatorWithNullErrors() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				TestBean tb = new TestBean();
				ValidationUtils.invokeValidator(new EmptyValidator(), tb, null);
			}
		}.runTest();
	}

	public void testInvokeValidatorSunnyDay() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BindException(tb, "tb");
		ValidationUtils.invokeValidator(new EmptyValidator(), tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	public void testValidationUtilsNull() throws Exception {
		TestBean tb = new TestBean();
		Errors errors = new BindException(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	public void testValidationUtilsEmpty() throws Exception {
		TestBean tb = new TestBean("");
		Errors errors = new BindException(tb, "tb");
		Validator testValidator = new EmptyValidator();
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY", errors.getFieldError("name").getCode());
	}

	public void testValidationUtilsSunnyDay() throws Exception {
		TestBean tb = new TestBean("");

		Validator testValidator = new EmptyValidator();
		tb.setName(" ");
		Errors errors = new BindException(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));

		tb.setName("Roddy");
		errors = new BindException(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));
	}

	public void testValidationUtilsEmptyOrWhitespace() throws Exception {
		//Test null
		TestBean tb = new TestBean();
		Errors errors = new BindException(tb, "tb");
		Validator testValidator = new ValidationUtilsEmptyOrWhitespaceValidator();
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		//Test empty String
		tb.setName("");
		errors = new BindException(tb, "tb");
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		//Test empty String
		tb.setName(" ");
		errors = new BindException(tb, "tb");
		testValidator.validate(tb, errors);
		assertTrue(errors.hasFieldErrors("name"));
		assertEquals("EMPTY_OR_WHITESPACE", errors.getFieldError("name").getCode());

		//Test OK
		tb.setName("Roddy");
		errors = new BindException(tb, "tb");
		testValidator.validate(tb, errors);
		assertFalse(errors.hasFieldErrors("name"));
	}


	private static final class EmptyValidator implements Validator {

		public boolean supports(Class clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		public void validate(Object obj, Errors errors) {
			ValidationUtils.rejectIfEmpty(errors, "name", "EMPTY", "You must enter a name!");
		}
	}

	private static final class ValidationUtilsEmptyOrWhitespaceValidator implements Validator {

		public boolean supports(Class clazz) {
			return TestBean.class.isAssignableFrom(clazz);
		}

		public void validate(Object obj, Errors errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "EMPTY_OR_WHITESPACE", "You must enter a name!");
		}
	}

}
