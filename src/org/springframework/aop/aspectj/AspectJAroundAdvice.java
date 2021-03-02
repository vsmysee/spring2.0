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

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Spring AOP around advice (MethodInterceptor) that wraps
 * an AspectJ advice method. Exposes ProceedingJoinPoint.
 *
 * @author Rod Johnson
 * @since 2.0
 */
public class AspectJAroundAdvice extends AbstractAspectJAdvice implements MethodInterceptor {
	
	public AspectJAroundAdvice(
			Method aspectJAroundAdviceMethod, AspectJExpressionPointcut pointcut,
			AspectInstanceFactory aif, ParameterNameDiscoverer parameterNameDiscoverer) {

		super(aspectJAroundAdviceMethod, pointcut, aif);
	}
	
	public boolean isBeforeAdvice() {
		return false;
	}
	
	public boolean isAfterAdvice() {
		return false;
	}


	public Object invoke(MethodInvocation mi) throws Throwable {
		ReflectiveMethodInvocation invocation = (ReflectiveMethodInvocation) mi;
		ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(invocation);
		JoinPointMatch jpm = getJoinPointMatch(invocation);
		return invokeAdviceMethod(pjp,jpm,null,null);
	}
	
	/**
	 * Return the ProceedingJoinPoint for the current invocation,
	 * instantiating it lazily if it hasn't already been bound to the
	 * thread
	 * @param rmi current Spring AOP ReflectiveMethodInvocation, which we'll
	 * use for attribute binding
	 * @return the ProceedingJoinPoint to make available to advice methods
	 */
	protected ProceedingJoinPoint lazyGetProceedingJoinPoint(ReflectiveMethodInvocation rmi) {
			return new MethodInvocationProceedingJoinPoint(rmi);
	}

}
