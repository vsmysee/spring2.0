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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

/**
 * Helper class for resolving constructors and factory methods.
 * Performs constructor resolution through argument matching.
 *
 * <p>Works on an AbstractBeanFactory and an InstantiationStrategy.
 * Used by AbstractAutowireCapableBeanFactory.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #autowireConstructor
 * @see #instantiateUsingFactoryMethod
 * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
 */
abstract class ConstructorResolver {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final AbstractBeanFactory beanFactory;

	private final InstantiationStrategy instantiationStrategy;


	/**
	 * Create a new ConstructorResolver for the given factory and instantiation strategy.
	 * @param beanFactory the BeanFactory to work with
	 * @param instantiationStrategy the instantiate strategy for creating bean instances
	 */
	public ConstructorResolver(AbstractBeanFactory beanFactory, InstantiationStrategy instantiationStrategy) {
		this.beanFactory = beanFactory;
		this.instantiationStrategy = instantiationStrategy;
	}


	/**
	 * "autowire constructor" (with constructor arguments by type) behavior.
	 * Also applied if explicit constructor argument values are specified,
	 * matching all remaining arguments with beans from the bean factory.
	 * <p>This corresponds to constructor injection: In this mode, a Spring
	 * bean factory is able to host components that expect constructor-based
	 * dependency resolution.
	 * @param beanName name of the bean
	 * @param mergedBeanDefinition the bean definition for the bean
	 * @return BeanWrapper for the new instance
	 */
	protected BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mergedBeanDefinition)
			throws BeansException {

		ConstructorArgumentValues cargs = mergedBeanDefinition.getConstructorArgumentValues();
		ConstructorArgumentValues resolvedValues = new ConstructorArgumentValues();

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		int minNrOfArgs = 0;
		if (cargs != null) {
			minNrOfArgs = resolveConstructorArguments(beanName, mergedBeanDefinition, cargs, resolvedValues);
		}

		Constructor[] candidates = mergedBeanDefinition.getBeanClass().getDeclaredConstructors();
		AutowireUtils.sortConstructors(candidates);

		Constructor constructorToUse = null;
		Object[] argsToUse = null;
		int minTypeDiffWeight = Integer.MAX_VALUE;

		for (int i = 0; i < candidates.length; i++) {
			Constructor constructor = candidates[i];

			if (constructorToUse != null &&
					constructorToUse.getParameterTypes().length > constructor.getParameterTypes().length) {
				// Already found greedy constructor that can be satisfied ->
				// do not look any further, there are only less greedy constructors left.
				break;
			}
			if (constructor.getParameterTypes().length < minNrOfArgs) {
				throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
						minNrOfArgs + " constructor arguments specified but no matching constructor found in bean '" +
						beanName + "' " +
						"(hint: specify index and/or type arguments for simple parameters to avoid type ambiguities)");
			}

			// Try to resolve arguments for current constructor.
			try {
				Class[] paramTypes = constructor.getParameterTypes();
				ArgumentsHolder args = createArgumentArray(
						beanName, mergedBeanDefinition, resolvedValues, bw, paramTypes, constructor);

				int typeDiffWeight = args.getTypeDifferenceWeight(paramTypes);
				// Choose this constructor if it represents the closest match.
				if (typeDiffWeight < minTypeDiffWeight) {
					constructorToUse = constructor;
					argsToUse = args.arguments;
					minTypeDiffWeight = typeDiffWeight;
				}
			}
			catch (UnsatisfiedDependencyException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring constructor [" + constructor + "] of bean '" + beanName +
							"': " + ex.getMessage());
				}
				if (i == candidates.length - 1 && constructorToUse == null) {
					throw ex;
				}
				else {
					// Swallow and try next constructor.
				}
			}
		}

		if (constructorToUse == null) {
			throw new BeanCreationException(
					mergedBeanDefinition.getResourceDescription(), beanName, "Could not resolve matching constructor");
		}

		Object beanInstance = this.instantiationStrategy.instantiate(
				mergedBeanDefinition, beanName, this.beanFactory, constructorToUse, argsToUse);
		bw.setWrappedInstance(beanInstance);
		if (logger.isDebugEnabled()) {
			logger.debug("Bean '" + beanName + "' instantiated via constructor [" + constructorToUse + "]");
		}
		return bw;
	}

	/**
	 * Instantiate the bean using a named factory method. The method may be static, if the
	 * mergedBeanDefinition parameter specifies a class, rather than a factoryBean, or
	 * an instance variable on a factory object itself configured using Dependency Injection.
	 * <p>Implementation requires iterating over the static or instance methods with the
	 * name specified in the RootBeanDefinition (the method may be overloaded) and trying
	 * to match with the parameters. We don't have the types attached to constructor args,
	 * so trial and error is the only way to go here. The explicitArgs array may contain
	 * argument values passed in programmatically via the corresponding getBean method.
	 * @param beanName name of the bean
	 * @param mergedBeanDefinition the bean definition for the bean
	 * @param explicitArgs argument values passed in programmatically via the getBean
	 * method, or <code>null</code> if none (-> use constructor argument values from bean definition)
	 * @return BeanWrapper for the new instance
	 */
	public BeanWrapper instantiateUsingFactoryMethod(
			String beanName, RootBeanDefinition mergedBeanDefinition, Object[] explicitArgs)
			throws BeansException {

		ConstructorArgumentValues cargs = mergedBeanDefinition.getConstructorArgumentValues();
		ConstructorArgumentValues resolvedValues = null;

		int minNrOfArgs = 0;
		if (explicitArgs == null) {
			// We don't have arguments passed in programmatically, so we need to resolve the
			// arguments specified in the constructor arguments held in the bean definition.
			resolvedValues = new ConstructorArgumentValues();
			minNrOfArgs = resolveConstructorArguments(beanName, mergedBeanDefinition, cargs, resolvedValues);
		}
		else {
			minNrOfArgs = explicitArgs.length;
		}

		boolean isStatic = true;
		Class factoryClass = null;
		Object factoryBean = null;

		if (mergedBeanDefinition.getFactoryBeanName() != null) {
			factoryBean = this.beanFactory.getBean(mergedBeanDefinition.getFactoryBeanName());
			factoryClass = factoryBean.getClass();
			isStatic = false;
		}
		else {
			// It's a static factory method on the bean class.
			factoryClass = mergedBeanDefinition.getBeanClass();
		}

		BeanWrapperImpl bw = new BeanWrapperImpl();
		this.beanFactory.initBeanWrapper(bw);

		// Try all methods with this name to see if they match the given arguments.
		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(factoryClass);

		Method factoryMethodToUse = null;
		Object[] argsToUse = null;
		int minTypeDiffWeight = Integer.MAX_VALUE;

		for (int i = 0; i < candidates.length; i++) {
			Method factoryMethod = candidates[i];

			if (Modifier.isStatic(factoryMethod.getModifiers()) == isStatic &&
					factoryMethod.getName().equals(mergedBeanDefinition.getFactoryMethodName()) &&
					factoryMethod.getParameterTypes().length >= minNrOfArgs) {

				Class[] paramTypes = factoryMethod.getParameterTypes();
				ArgumentsHolder args = null;

				if (resolvedValues != null) {
					// Resolved contructor arguments: type conversion and/or autowiring necessary.
					try {
						args = createArgumentArray(
								beanName, mergedBeanDefinition, resolvedValues, bw, paramTypes, factoryMethod);
					}
					catch (UnsatisfiedDependencyException ex) {
						if (logger.isDebugEnabled()) {
							logger.debug("Ignoring factory method [" + factoryMethod + "] of bean '" + beanName +
									"': " + ex.getMessage());
						}
						if (i == candidates.length - 1 && factoryMethodToUse == null) {
							throw ex;
						}
						else {
							// Swallow and try next overloaded factory method.
							continue;
						}
					}
				}

				else {
					// Explicit arguments given -> arguments length must match exactly.
					if (paramTypes.length != explicitArgs.length) {
						continue;
					}
					args = new ArgumentsHolder(explicitArgs);
				}

				int typeDiffWeight = args.getTypeDifferenceWeight(paramTypes);
				// Choose this constructor if it represents the closest match.
				if (typeDiffWeight < minTypeDiffWeight) {
					factoryMethodToUse = factoryMethod;
					argsToUse = args.arguments;
					minTypeDiffWeight = typeDiffWeight;
				}
			}
		}
		
		if (factoryMethodToUse == null) {
			throw new BeanDefinitionStoreException("No matching factory method found: " +
					(mergedBeanDefinition.getFactoryBeanName() != null ?
					 "factory bean '" + mergedBeanDefinition.getFactoryBeanName() + "'; " : "") +
					"factory method '" + mergedBeanDefinition.getFactoryMethodName() + "'");
		}
		if (!factoryMethodToUse.isAccessible()) {
			factoryMethodToUse.setAccessible(true);
		}

		Object beanInstance = this.instantiationStrategy.instantiate(
				mergedBeanDefinition, beanName, this.beanFactory, factoryBean, factoryMethodToUse, argsToUse);
		if (beanInstance == null) {
			throw new BeanCreationException(
					mergedBeanDefinition.getResourceDescription(), beanName,
					"Factory method '" + mergedBeanDefinition.getFactoryMethodName() + "' on class [" +
					factoryClass.getName() + "] returned null");
		}

		bw.setWrappedInstance(beanInstance);
		if (logger.isDebugEnabled()) {
			logger.debug("Bean '" + beanName + "' instantiated via factory method '" + factoryMethodToUse + "'");
		}
		return bw;
	}

	/**
	 * Resolve the constructor arguments for this bean into the resolvedValues object.
	 * This may involve looking up other beans.
	 * This method is also used for handling invocations of static factory methods.
	 */
	private int resolveConstructorArguments(
			String beanName, RootBeanDefinition mergedBeanDefinition,
			ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

		BeanDefinitionValueResolver valueResolver =
				new BeanDefinitionValueResolver(this.beanFactory, beanName, mergedBeanDefinition);

		int minNrOfArgs = cargs.getArgumentCount();

		for (Iterator it = cargs.getIndexedArgumentValues().entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			int index = ((Integer) entry.getKey()).intValue();
			if (index < 0) {
				throw new BeanCreationException(mergedBeanDefinition.getResourceDescription(), beanName,
						"Invalid constructor argument index: " + index);
			}
			if (index > minNrOfArgs) {
				minNrOfArgs = index + 1;
			}
			String argName = "constructor argument with index " + index;
			ConstructorArgumentValues.ValueHolder valueHolder =
					(ConstructorArgumentValues.ValueHolder) entry.getValue();
			Object resolvedValue = valueResolver.resolveValueIfNecessary(argName, valueHolder.getValue());
			resolvedValues.addIndexedArgumentValue(index, resolvedValue, valueHolder.getType());
		}

		for (Iterator it = cargs.getGenericArgumentValues().iterator(); it.hasNext();) {
			ConstructorArgumentValues.ValueHolder valueHolder =
					(ConstructorArgumentValues.ValueHolder) it.next();
			String argName = "constructor argument";
			Object resolvedValue = valueResolver.resolveValueIfNecessary(argName, valueHolder.getValue());
			resolvedValues.addGenericArgumentValue(resolvedValue, valueHolder.getType());
		}

		return minNrOfArgs;
	}

	/**
	 * Create an array of arguments to invoke a constructor or factory method,
	 * given the resolved constructor argument values.
	 */
	private ArgumentsHolder createArgumentArray(
			String beanName, RootBeanDefinition mergedBeanDefinition, ConstructorArgumentValues resolvedValues,
			BeanWrapperImpl bw, Class[] paramTypes, Object methodOrCtor)
			throws UnsatisfiedDependencyException {

		String methodType = (methodOrCtor instanceof Constructor ? "constructor" : "factory method");

		ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
		Set usedValueHolders = new HashSet(paramTypes.length);

		for (int index = 0; index < paramTypes.length; index++) {
			// Try to find matching constructor argument value, either indexed or generic.
			ConstructorArgumentValues.ValueHolder valueHolder =
					resolvedValues.getArgumentValue(index, paramTypes[index], usedValueHolders);
			// If we couldn't find a direct match and are not supposed to autowire,
			// let's try the next generic, untyped argument value as fallback:
			// it could match after type conversion (for example, String -> int).
			if (valueHolder == null &&
					mergedBeanDefinition.getResolvedAutowireMode() != RootBeanDefinition.AUTOWIRE_CONSTRUCTOR) {
				valueHolder = resolvedValues.getGenericArgumentValue(null, usedValueHolders);
			}
			if (valueHolder != null) {
				// We found a potential match - let's give it a try.
				// Do not consider the same value definition multiple times!
				usedValueHolders.add(valueHolder);
				args.rawArguments[index] = valueHolder.getValue();
				try {
					args.arguments[index] =
							this.beanFactory.doTypeConversionIfNecessary(bw, args.rawArguments[index],
									paramTypes[index], MethodParameter.forMethodOrConstructor(methodOrCtor, index));
				}
				catch (TypeMismatchException ex) {
					throw new UnsatisfiedDependencyException(
							mergedBeanDefinition.getResourceDescription(), beanName, index, paramTypes[index],
							"Could not convert " + methodType + " argument value [" + valueHolder.getValue() +
							"] to required type [" + paramTypes[index].getName() + "]: " + ex.getMessage());
				}
			}
			else {
				// No explicit match found: we're either supposed to autowire or
				// have to fail creating an argument array for the given constructor.
				if (mergedBeanDefinition.getResolvedAutowireMode() != RootBeanDefinition.AUTOWIRE_CONSTRUCTOR) {
					throw new UnsatisfiedDependencyException(
							mergedBeanDefinition.getResourceDescription(), beanName, index, paramTypes[index],
							"Ambiguous " + methodType + " argument types - " +
							"did you specify the correct bean references as " + methodType + " arguments?");
				}
				Map matchingBeans = findMatchingBeans(paramTypes[index]);
				if (matchingBeans == null || matchingBeans.size() != 1) {
					int matchingBeansCount = (matchingBeans != null ? matchingBeans.size() : 0);
					throw new UnsatisfiedDependencyException(
							mergedBeanDefinition.getResourceDescription(), beanName, index, paramTypes[index],
							"There are " + matchingBeansCount + " beans of type [" + paramTypes[index] +
							"] for autowiring " + methodType + ". There should have been 1 to be able to " +
							"autowire " + methodType + " of bean '" + beanName + "'.");
				}
				String autowiredBeanName = (String) matchingBeans.keySet().iterator().next();
				Object autowiredBean = matchingBeans.values().iterator().next();
				args.rawArguments[index] = autowiredBean;
				args.arguments[index] = autowiredBean;
				if (mergedBeanDefinition.isSingleton()) {
					this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Autowiring by type from bean name '" + beanName +
							"' via " + methodType + " to bean named '" + autowiredBeanName + "'");
				}
			}
		}
		return args;
	}


	/**
	 * Find bean instances that match the required type. Called by autowiring.
	 * If a subclass cannot obtain information about bean names by type,
	 * a corresponding exception should be thrown.
	 * @param requiredType the type of the beans to look up
	 * @return a Map of bean names and bean instances that match the required type,
	 * or <code>null</code> if none found
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected abstract Map findMatchingBeans(Class requiredType) throws BeansException;


	/**
	 * Private inner class for holding argument combinations.
	 */
	private static class ArgumentsHolder {

		public Object rawArguments[];

		public Object arguments[];

		public ArgumentsHolder(int size) {
			this.rawArguments = new Object[size];
			this.arguments = new Object[size];
		}

		public ArgumentsHolder(Object[] args) {
			this.rawArguments = args;
			this.arguments = args;
		}

		public int getTypeDifferenceWeight(Class[] paramTypes) {
			// If valid arguments found, determine type difference weight.
			// Try type difference weight on both the converted arguments and
			// the raw arguments. If the raw weight is better, use it.
			// Decrease raw weight by 1024 to prefer it over equal converted weight.
			int typeDiffWeight = AutowireUtils.getTypeDifferenceWeight(paramTypes, this.arguments);
			int rawTypeDiffWeight = AutowireUtils.getTypeDifferenceWeight(paramTypes, this.rawArguments) - 1024;
			return (rawTypeDiffWeight < typeDiffWeight ? rawTypeDiffWeight : typeDiffWeight);
		}
	}

}
