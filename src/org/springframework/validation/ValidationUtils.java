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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class offering convenient methods for invoking a {@link Validator}
 * and for rejecting empty fields.
 * 
 * <p>Checks for an empty field in <code>Validator</code> implementations
 * can thus become one-liners.
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 06.05.2003
 * @see Validator
 * @see org.springframework.validation.Errors
 */
public abstract class ValidationUtils {

	private static Log logger = LogFactory.getLog(ValidationUtils.class);


	/**
	 * Invoke the given {@link Validator} for the supplied object and
	 * {@link org.springframework.validation.Errors} instance.
	 * @param validator the <code>Validator</code> to be invoked (must not be <code>null</code>)
	 * @param obj the object to bind the parameters to
	 * @param errors the {@link org.springframework.validation.Errors} instance that should store the errors (must not be <code>null</code>)
	 * @throws IllegalArgumentException if either of the <code>Validator</code> or <code>Errors</code> arguments is <code>null</code>;
	 * or if the supplied <code>Validator</code> does not {@link Validator#supports(Class) support}
	 * the validation of the supplied object's type
	 */
	public static void invokeValidator(Validator validator, Object obj, Errors errors) {
		Assert.notNull(validator, "Validator must not be null");
		Assert.notNull(errors, "Errors object must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Invoking validator [" + validator + "]");
		}
		if (obj != null && !validator.supports(obj.getClass())) {
			throw new IllegalArgumentException("Validator " + validator.getClass() +
					" does not support " + obj.getClass());
		}
		validator.validate(obj, errors);
		if (logger.isDebugEnabled()) {
			if (errors.hasErrors()) {
				logger.debug("Validator found " + errors.getErrorCount() + " errors");
			}
			else {
				logger.debug("Validator found no errors");
			}
		}
	}
	
	/**
	 * Reject the given field with the given error code if the value is empty.
	 * <p>An 'empty' value in this context means either <code>null</code> or
	 * the empty string "". 
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode) {
		rejectIfEmpty(errors, field, errorCode, null, null);
	}

	/**
	 * Reject the given field with the given error code and default message
	 * if the value is empty.
	 * <p>An 'empty' value in this context means either <code>null</code> or
	 * the empty string "". 
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode error code, interpretable as message key
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmpty(Errors errors, String field, String errorCode, String defaultMessage) {
		rejectIfEmpty(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * Reject the given field with the given error code, error arguments
	 * and default message if the value is empty.
	 * <p>An 'empty' value in this context means either <code>null</code> or
	 * the empty string "". 
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be <code>null</code>)
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmpty(
			Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null || !StringUtils.hasLength(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

	/**
	 * Reject the given field with the given error code if the value is empty
	 * or just contains whitespace.
	 * <p>An 'empty' value in this context means either <code>null</code>,
	 * the empty string "", or consisting wholly of whitespace.
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 */
	public static void rejectIfEmptyOrWhitespace(Errors errors, String field, String errorCode) {
		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, null);
	}

	/**
	 * Reject the given field with the given error code and default message
	 * if the value is empty or just contains whitespace.
	 * <p>An 'empty' value in this context means either <code>null</code>,
	 * the empty string "", or consisting wholly of whitespace.
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, String defaultMessage) {

		rejectIfEmptyOrWhitespace(errors, field, errorCode, null, defaultMessage);
	}

	/**
	 * Reject the given field with the given error code, error arguments
	 * and default message if the value is empty or just contains whitespace.
	 * <p>An 'empty' value in this context means either <code>null</code>,
	 * the empty string "", or consisting wholly of whitespace.
	 * <p>The object whose field is being validated does not need to be passed
	 * in because the {@link org.springframework.validation.Errors} instance can resolve field values by itself
	 * (it will usually hold an internal reference to the target object).
	 * @param errors the <code>Errors</code> instance to register errors on
	 * @param field the field name to check
	 * @param errorCode the error code, interpretable as message key
	 * @param errorArgs the error arguments, for argument binding via MessageFormat
	 * (can be <code>null</code>)
	 * @param defaultMessage fallback default message
	 */
	public static void rejectIfEmptyOrWhitespace(
			Errors errors, String field, String errorCode, Object[] errorArgs, String defaultMessage) {

		Assert.notNull(errors, "Errors object must not be null");
		Object value = errors.getFieldValue(field);
		if (value == null ||!StringUtils.hasText(value.toString())) {
			errors.rejectValue(field, errorCode, errorArgs, defaultMessage);
		}
	}

}
