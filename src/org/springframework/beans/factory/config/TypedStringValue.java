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

package org.springframework.beans.factory.config;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Holder for a typed String value. Can be added to bean definitions
 * to explicitly specify a target type for a String value, for example
 * for collection elements.
 *
 * <p>This holder will just store the String value and the target type.
 * The actual conversion will be performed by the bean factory.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.beans.factory.config.BeanDefinition#getPropertyValues
 * @see org.springframework.beans.MutablePropertyValues#addPropertyValue
 */
public class TypedStringValue {

	private String value;

	private Object targetType;


	/**
	 * Create a new {@link org.springframework.beans.factory.config.TypedStringValue} for the given String
	 * value and target type.
	 * @param value the String value
	 * @param targetType the type to convert to
	 */
	public TypedStringValue(String value, Class targetType) {
		setValue(value);
		setTargetType(targetType);
	}

	/**
	 * Create a new {@link org.springframework.beans.factory.config.TypedStringValue} for the given String
	 * value and target type.
	 * @param value the String value
	 * @param targetTypeName the type to convert to
	 */
	public TypedStringValue(String value, String targetTypeName) {
		setValue(value);
		setTargetTypeName(targetTypeName);
	}


	/**
	 * Set the String value.
	 * Only necessary for manipulating a registered value,
	 * for example in BeanFactoryPostProcessors.
	 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Return the String value.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Return whether this typed String value carries a target type .
	 */
	public boolean hasTargetType() {
		return (this.targetType instanceof Class);
	}

	/**
	 * Set the type to convert to.
	 * Only necessary for manipulating a registered value,
	 * for example in BeanFactoryPostProcessors.
	 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
	 */
	public void setTargetType(Class targetType) {
		Assert.notNull(targetType, "targetType is required");
		this.targetType = targetType;
	}

	/**
	 * Return the type to convert to.
	 */
	public Class getTargetType() {
		if (!(this.targetType instanceof Class)) {
			throw new IllegalStateException("Typed String value does not carry a resolved target type");
		}
		return (Class) this.targetType;
	}

	/**
	 * Specify the type to convert to.
	 */
	public void setTargetTypeName(String targetTypeName) {
		Assert.notNull(targetTypeName, "targetTypeName is required");
		this.targetType = targetTypeName;
	}

	/**
	 * Return the type to convert to.
	 */
	public String getTargetTypeName() {
		if (this.targetType instanceof Class) {
			return ((Class) this.targetType).getName();
		}
		else {
			return (String) this.targetType;
		}
	}

	/**
	 * Determine the type to convert to, resolving it from a specified class name
	 * if necessary. Will also reload a specified Class from its name when called
	 * with the target type already resolved.
	 * @param classLoader the ClassLoader to use for resolving a (potential) class name
	 * @return the resolved type to convert to
	 */
	public Class resolveTargetType(ClassLoader classLoader) throws ClassNotFoundException {
		if (this.targetType == null) {
			return null;
		}
		Class resolvedClass = ClassUtils.forName(getTargetTypeName(), classLoader);
		this.targetType = resolvedClass;
		return resolvedClass;
	}

}
