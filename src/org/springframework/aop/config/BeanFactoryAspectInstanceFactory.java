/*
 * Copyright 2002-2005 the original author or authors.
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

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link org.springframework.aop.aspectj.AspectInstanceFactory} that locates the aspect from the
 * {@link org.springframework.beans.factory.BeanFactory} using a configured bean name.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class BeanFactoryAspectInstanceFactory implements AspectInstanceFactory, BeanFactoryAware {

	private String aspectBeanName;

	private BeanFactory beanFactory;

	private int instantiationCount;


	/**
	 * Set the name of the aspect bean. This is the bean that is returned when calling
	 * {@link #getAspectInstance()}.
	 */
	public void setAspectBeanName(String aspectBeanName) {
		this.aspectBeanName = aspectBeanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		if (!StringUtils.hasText(this.aspectBeanName)) {
			throw new IllegalArgumentException("Property 'aspectBeanName' is required");
		}
	}


	/**
	 * Look up the aspect bean from the {@link BeanFactory} and returns it.
	 * @see #setAspectBeanName
	 */
	public Object getAspectInstance() {
		this.instantiationCount++;
		return this.beanFactory.getBean(this.aspectBeanName);
	}

	public int getInstantiationCount() {
		return instantiationCount;
	}

}
