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

package org.springframework.aop.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.OrderComparator;
import org.springframework.util.ClassUtils;

/**
 * Utility methods used by the AOP framework and by AOP proxy factories.
 * Mainly for internal use within the framework.
 *
 * <p>See AopProxyUtils for a collection of framework-specific AOP
 * utility methods which depend on the AOP framework itself.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see org.springframework.aop.framework.AopProxyUtils
 */
public abstract class AopUtils {
	
	private static Log logger = LogFactory.getLog(AopUtils.class);


	/**
	 * Return whether the given object is either a J2SE dynamic
	 * proxy or a CGLIB proxy.
	 * @param object the object to check
	 * @see #isJdkDynamicProxy
	 * @see #isCglibProxy
	 */
	public static boolean isAopProxy(Object object) {
		return isJdkDynamicProxy(object) || isCglibProxy(object);
	}

	/**
	 * Return whether the given object is a J2SE dynamic proxy.
	 * @param object the object to check
	 * @see java.lang.reflect.Proxy#isProxyClass
	 */
	public static boolean isJdkDynamicProxy(Object object) {
		return (object != null && Proxy.isProxyClass(object.getClass()));
	}

	/**
	 * Return whether the given object is a CGLIB proxy.
	 * @param object the object to check
	 */
	public static boolean isCglibProxy(Object object) {
		return (object != null && isCglibProxyClass(object.getClass()));
	}
    
	/**
	 * Return whether the specified class is a CGLIB-generated class.
	 * @param clazz the class to check
	 */
	public static boolean isCglibProxyClass(Class clazz) {
		return (clazz != null && clazz.getName().indexOf("$$") != -1);
	}

	/**
	 * Return whether the given method is an "equals" method.
	 * @see Object#equals
	 */
	public static boolean isEqualsMethod(Method method) {
		return (method != null && method.getName().equals("equals") &&
				method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class);
	}

	/**
	 * Return whether the given method is a "hashCode" method.
	 * @see Object#hashCode
	 */
	public static boolean isHashCodeMethod(Method method) {
		return (method != null && method.getName().equals("hashCode") &&
				method.getParameterTypes().length == 0);
	}

	/**
	 * Return whether the given method is a "toString" method.
	 * @see Object#toString()
	 */
	public static boolean isToStringMethod(Method method) {
		return (method != null && method.getName().equals("toString") &&
				method.getParameterTypes().length == 0);
	}

	/**
	 * Given a method, which may come from an interface, and a targetClass
	 * used in the current AOP invocation, find the most specific method
	 * if there is one. E.g. the method may be IFoo.bar() and the target
	 * class may be DefaultFoo. In this case, the method may be
	 * DefaultFoo.bar(). This enables attributes on that method to be found.
	 * @param method method to be invoked, which may come from an interface
	 * @param targetClass target class for the curren invocation. May
	 * be <code>null</code> or may not even implement the method.
	 * @return the more specific method, or the original method if the
	 * targetClass doesn't specialize it or implement it or is null
	 */
	public static Method getMostSpecificMethod(Method method, Class targetClass) {
		if (method != null && targetClass != null) {
			try {
				method = targetClass.getMethod(method.getName(), method.getParameterTypes());
			}
			catch (NoSuchMethodException ex) {
				// Perhaps the target class doesn't implement this method:
				// that's fine, just use the original method
			}
		}
		return method;
	}

	/**
	 * Can the given pointcut apply at all on the given class?
	 * This is an important test as it can be used to optimize
	 * out a pointcut for a class.
	 * @param pc pc static or dynamic pointcut to check
	 * @param targetClass class we're testing
	 * @return whether the pointcut can apply on any method
	 */
	public static boolean canApply(Pointcut pc, Class targetClass) {
		return canApply(pc, targetClass, false);
	}

	/**
	 * Can the given pointcut apply at all on the given class?
	 * This is an important test as it can be used to optimize
	 * out a pointcut for a class.
	 * @param pc pc static or dynamic pointcut to check
	 * @param targetClass class we're testing
	 * @param hasIntroductions whether or not the advisor chain for this bean includes
	 * any introductions
	 * @return whether the pointcut can apply on any method
	 */
	public static boolean canApply(Pointcut pc, Class targetClass, boolean hasIntroductions) {
		if (!pc.getClassFilter().matches(targetClass)) {
			return false;
		}

		MethodMatcher methodMatcher = pc.getMethodMatcher();
		IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
		if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
			introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
		}
		
		Set classes = new HashSet(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
		classes.add(targetClass);
		for (Iterator it = classes.iterator(); it.hasNext();) {
			Class clazz = (Class) it.next();
			Method[] methods = clazz.getMethods();
			for (int j = 0; j < methods.length; j++) {
				if (introductionAwareMethodMatcher != null) {
					if (introductionAwareMethodMatcher.matches(methods[j], targetClass, hasIntroductions)) {
						return true;
					}
				}
				else if (methodMatcher.matches(methods[j], targetClass)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Can the given advisor apply at all on the given class?
	 * This is an important test as it can be used to optimize
	 * out a advisor for a class.
	 * @param advisor the advisor to check
	 * @param targetClass class we're testing
	 * @return whether the pointcut can apply on any method
	 */
	public static boolean canApply(Advisor advisor, Class targetClass) {
		return canApply(advisor, targetClass, false);
	}
	
	/**
	 * Can the given advisor apply at all on the given class?
	 * This is an important test as it can be used to optimize
	 * out a advisor for a class.
	 * This version also takes into account introductions, for
	 * IntroductionAwareMethodMatchers
	 * @param advisor the advisor to check
	 * @param targetClass class we're testing
	 * @param hasIntroductions whether or not the advisor chain for this bean includes
	 * any introductions
	 * @return whether the pointcut can apply on any method
	 */
	public static boolean canApply(Advisor advisor, Class targetClass, boolean hasIntroductions) {
		if (advisor instanceof IntroductionAdvisor) {
			return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
		}
		else if (advisor instanceof PointcutAdvisor) {
			PointcutAdvisor pca = (PointcutAdvisor) advisor;
			return canApply(pca.getPointcut(), targetClass, hasIntroductions);
		}
		else {
			// It doesn't have a pointcut so we assume it applies
			return true;
		}
	}

	
	/**
	 * Convenience method to return the sublist of the candidateAdvisors list
	 * that are applicable to the given class.
	 * @param candidateAdvisors advisors to evaluate
	 * @param clazz target class
	 * @return sublist of advisors that could apply to an object of the given class
	 */
	public static List findAdvisorsThatCanApply(List candidateAdvisors, Class clazz) {
		List eligibleAdvisors = findIntroductionAdvisorsThatCanApply(candidateAdvisors, clazz);
		boolean hasIntroductions = !eligibleAdvisors.isEmpty();
		for (Iterator it = candidateAdvisors.iterator(); it.hasNext();) {
			Advisor candidate = (Advisor) it.next();
			if (candidate instanceof IntroductionAdvisor) {
				// already processed
				continue;
			}
			if (AopUtils.canApply(candidate, clazz, hasIntroductions)) {
				eligibleAdvisors.add(candidate);
				if (logger.isDebugEnabled()) {
					logger.debug("Candidate advisor [" + candidate + "] accepted for class [" + clazz.getName() + "]");
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Candidate advisor [" + candidate + "] rejected for class [" + clazz.getName() + "]");
				}
			}
		}
		return eligibleAdvisors;
	}
	
	private static List findIntroductionAdvisorsThatCanApply(List candidateAdvisors, Class clazz) {
		List eligibleAdvisors = new LinkedList();
		for (Iterator it = candidateAdvisors.iterator(); it.hasNext();) {
			Advisor candidate = (Advisor) it.next();
			if ((candidate instanceof IntroductionAdvisor) && AopUtils.canApply(candidate, clazz)) {
				eligibleAdvisors.add(candidate);
				if (logger.isDebugEnabled()) {
					logger.debug("Candidate advisor [" + candidate + "] accepted for class [" + clazz.getName() + "]");
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Candidate advisor [" + candidate + "] rejected for class [" + clazz.getName() + "]");
				}
			}
		}
		return eligibleAdvisors;		
	}
	
	/**
	 * Sort the given list of advisors by order value
	 * @param advisors Spring AOP advisors to sort
	 * @return sorted list of advisors
	 */
	public static List sortAdvisorsByOrder(List advisors) {
		Collections.sort(advisors, new OrderComparator());
		return advisors;
	}


	/**
	 * Invoke the target directly via reflection.
	 * @param target the target object
	 * @param method the method to invoke
	 * @param args the arguments for the method
	 * @throws Throwable if thrown by the target method
	 * @throws org.springframework.aop.AopInvocationException in case of a reflection error
	 */
	public static Object invokeJoinpointUsingReflection(Object target, Method method, Object[] args)
	    throws Throwable {

		// Use reflection to invoke the method.
		try {
			if (!Modifier.isPublic(method.getModifiers()) ||
					!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
				method.setAccessible(true);
			}
			return method.invoke(target, args);
		}
		catch (InvocationTargetException ex) {
			// Invoked method threw a checked exception.
			// We must rethrow it. The client won't see the interceptor.
			throw ex.getTargetException();
		}
		catch (IllegalArgumentException ex) {
			throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
					method + "] on target [" + target + "]", ex);
		}
		catch (IllegalAccessException ex) {
			throw new AopInvocationException("Couldn't access method: " + method, ex);
		}
	}

}
