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

/**
 * Interface that defines a registry for shared bean instances.
 * Can be implemented by BeanFactory implementations to expose
 * their singleton management facility in a uniform manner.
 *
 * <p>The ConfigurableBeanFactory interface extends this interface.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 */
public interface SingletonBeanRegistry {

	/**
	 * Register the given existing object as singleton in the bean registry,
	 * under the given bean name.
	 * <p>The given instance is supposed to be fully initialized; the registry
	 * will not perform any initialization callbacks (in particular, it won't
	 * call InitializingBean's <code>afterPropertiesSet</code> method).
	 * The given instance will not receive any destruction callbacks
	 * (like DisposableBean's <code>destroy</code> method) either.
	 * <p>If running within a full BeanFactory: <b>Register a bean definition
	 * instead of an existing instance if your bean is supposed to receive
	 * initialization and/or destruction callbacks.</b>
	 * <p>Typically invoked during registry configuration, but can also be used
	 * for runtime registration of singletons. As a consequence, a registry
	 * implementation should synchronize singleton access; it will have to do
	 * this anyway if it supports a BeanFactory's lazy initialization of singletons.
	 * @param beanName name of the bean
	 * @param singletonObject the existing object
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 * @see org.springframework.beans.factory.DisposableBean#destroy
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
	 */
	void registerSingleton(String beanName, Object singletonObject);

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * Only checks already instantiated singletons; does not return an Object
	 * for singleton bean definitions that have not been instantiated yet.
	 * <p>The main purpose of this method is to access manually registered singletons
	 * (see <code>registerSingleton</code>). Can also be used to access a singleton
	 * defined by a bean definition that already been created, in a raw fashion.
	 * @param beanName the name of the bean
	 * @return the registered singleton object, or <code>null</code> if none
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinition
	 */
	Object getSingleton(String beanName);

	/**
	 * Check if this registry contains a singleton instance with the given name.
	 * Only checks already instantiated singletons; does not return <code>true</code>
	 * for singleton bean definitions that have not been instantiated yet.
	 * <p>The main purpose of this method is to check manually registered singletons
	 * (see <code>registerSingleton</code>). Can also be used to check whether a
	 * singleton defined by a bean definition has already been created.
	 * <p>To check whether a bean factory contains a bean definition with a given name,
	 * use ListableBeanFactory's <code>containsBeanDefinition</code>. Calling both
	 * <code>containsBeanDefinition</code> and <code>containsSingleton</code> answers
	 * whether a specific bean factory contains an own bean with the given name.
	 * <p>Use BeanFactory's <code>containsBean</code> for general checks whether the
	 * factory knows about a bean with a given name (whether manually registered singleton
	 * instance or created by bean definition), also checking ancestor factories.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a singleton instance with the given name
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#containsBeanDefinition
	 * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
	 * @see org.springframework.beans.factory.BeanFactory#containsBean
	 */
	boolean containsSingleton(String beanName);

	/**
	 * Return the names of singleton beans registered in this registry.
	 * Only checks already instantiated singletons; does not return names
	 * for singleton bean definitions that have not been instantiated yet.
	 * <p>The main purpose of this method is to check manually registered singletons
	 * (see <code>registerSingleton</code>). Can also be used to check which
	 * singletons defined by a bean definition have already been created.
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionNames
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames
	 */
	String[] getSingletonNames();

	/**
	 * Return the number of singleton beans registered in this registry.
	 * Only checks already instantiated singletons; does not count
	 * singleton bean definitions that have not been instantiated yet.
	 * <p>The main purpose of this method is to check manually registered singletons
	 * (see <code>registerSingleton</code>). Can also be used to count the number
	 * of singletons defined by a bean definition that have already been created.
	 * @see #registerSingleton
	 * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionCount
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount
	 */
	int getSingletonCount();

}
