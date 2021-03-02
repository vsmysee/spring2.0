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

package org.springframework.aop.aspectj;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Implementation of ParameterNameDiscover that tries to deduce
 * parameter names for an advice method from the pointcut expression,
 * returning, and throwing clauses. If an unambiguous interpretation is
 * not avaliable, it will return null.
 * </p>
 * <p>
 * This class interprets arguments in the following way:
 * </p>
 * <ol>
 *   <li>If the first parameter of the method is of type JoinPoint or ProceedingJoinPoint, it
 *   is assumed to be for passing thisJoinPoint to the advice, and the parameter name will be assigned the
 *   value "thisJoinPoint".</li>
 *   <li>If the first parameter of the method is of type JoinPoint.StaticPart it is assumed to be
 *   for passing "thisJoinPointStaticPart" to the advice, and the parameter name will be assigned the
 *   value "thisJoinPointStaticPart"</li>
 *   <li>If a throwingName has been set, and there are no unbound arguments of type Throwable+, 
 *   then an IllegalArgumentCondition is raised. If there is more than one unbound argument of type
 *   Throwable+, then an AmbiguousBindingCondition is raised. If there is exactly one unbound argument
 *   of type Throwable+, then the corresponding parameter name is assigned the value &lt;throwingName&gt;.
 *   </li>
 *   <li>If there remain unbound arguments, then the pointcut expression is examined. Let <code>a</code>
 *   be the number of annotation-based pointcut expressions (&#64;annotation, &#64;this, &#64;target,
 *   &#64;args, &#64;within, &#64;withincode) that are used in binding form. Usage in binding form
 *   has itself to be deduced: if the expression inside the pointcut is a single string literal that meets
 *   Java variable name conventions it is assumed to be a variable name. 
 *   If <code>a</code> is zero we proceed to the next stage. If <code>a</code> &gt; 1 then an 
 *   AmbiguousBindingCondition is raised. If <code>a</code> == 1, and there are no unbound arguments of
 *   type Annotation+, then an IllegalArgumentCondition is raised. if there is exactly one such argument,
 *   then the corresponding parameter name is assigned the value from the pointcut expression.
 *   </li>
 *   <li>If a returningName has been set, and there are no unbound arguments
 *   then an IllegalArgumentCondition is raised. If there is more than one unbound argument then 
 *   an AmbiguousBindingCondition is raised. If there is exactly one unbound argument then the 
 *   corresponding parameter name is assigned the value &lt;returningName&gt;.
 *   </li>
 *   </li>If there remain unbound arguments, then the pointcut expression is examined once more for
 *   this, target, and args pointcut expressions used in the binding form (binding forms
 *   are deduced as described for the annotation based pointcuts). If there remains more than one
 *   unbound argument of a primitive type (which can only be bound in args) then an AmbiguousBindingCondition
 *   is raised. If there is exactly one argument of a primitive type, then if exactly one args bound
 *   variable was found, we assign the corresponding parameter name the variable name. If there were no args
 *   bound variables found an IllegalStateCondition is raised. If there are multiple args bound variables,
 *   an AmbiguousBindingCondition is raised. At this point, if there remains more than one unbound argument
 *   we raise an AmbiguousBindingCondition. If there are no unbound arguments remaining, we are done. If there
 *   is exactly one unbound argument remaining, and only one candidate variable name unbound from this, target,
 *   or args, it is assigned as the corresponding parameter name. If there are multiple possibilities, an
 *   AmbiguousBindingCondition is raised.
 * </ol>
 * <p>The behaviour on raising an IllegalArgumentCondition or AmbiguousBindingConfiguration is configurable to
 * allow this discoverer to be used as part of a chain-of-responsibility. By default the condition will be logged
 * and the getParameterNames method will simply return null. If the raiseExceptions property is set to true,
 * the conditions will be thrown as IllegalArgumentException and AmbiguousBindingException respectively.</p>
 * 
 * <p>Was that perfectly clear?? ;) </p>
 * <p>Short version: if an unambiguous binding can be deduced, then it is. If the advice requirements cannot
 * possibly be satisfied null is returned. By setting the raiseExceptions property to true, more descriptive
 * exceptions will be thrown instead of returning null in the case that the parameter names cannot be discovered.
 * </p>
 * 
 * @author Adrian Colyer
 * @since 2.0
 */
public class AspectJAdviceParameterNameDiscoverer implements ParameterNameDiscoverer {
	
	private static final String ANNOTATION_CLASS_NAME = "java.lang.annotation.Annotation";

	private static final String THIS_JOIN_POINT = "thisJoinPoint";
	private static final String THIS_JOIN_POINT_STATIC_PART = "thisJoinPointStaticPart";

	// steps in the binding algorithm...
	private static final int STEP_JOIN_POINT_BINDING = 1;
	private static final int STEP_THROWING_BINDING = 2;
	private static final int STEP_ANNOTATION_BINDING = 3;
	private static final int STEP_RETURNING_BINDING = 4;
	private static final int STEP_PRIMITIVE_ARGS_BINDING = 5;
	private static final int STEP_THIS_TARGET_ARGS_BINDING = 6;
	private static final int STEP_FINISHED = 7;
	
	private static final Set singleValuedAnnotationPcds = new HashSet();

	private static Class annotationClass;
	
	static {
		singleValuedAnnotationPcds.add("@this");
		singleValuedAnnotationPcds.add("@target");
		singleValuedAnnotationPcds.add("@within");
		singleValuedAnnotationPcds.add("@withincode");
		singleValuedAnnotationPcds.add("@annotation");
		
		try {
			annotationClass = Class.forName(ANNOTATION_CLASS_NAME);
		}
		catch(ClassNotFoundException ex) {
			// Running on < JDK 1.5, this is ok...
			annotationClass = null;
		}
	}


	private boolean raiseExceptions = false;
	
	/**
	 * If the advice is afterReturning, and binds the return value, this is the parameter name used. 
	 */
	private String returningName = null;
	
	/**
	 * If the advice is afterThrowing, and binds the thrown value, this is the parameter name used.
	 */
	private String throwingName = null;
	
	/**
	 * The pointcut expression associated with the advice, as a simple String.
	 */
	private String pointcutExpression = null;
	
	private Class[] argumentTypes;

	private String[] parameterNameBindings;

	private int numberOfRemainingUnboundArguments;

	private int algorithmicStep = STEP_JOIN_POINT_BINDING;
	
	
	/**
	 * Create a new discoverer that attempts to discover parameter names from the given
	 * pointcut expression.
	 * @param pointcutExpression
	 */
	public AspectJAdviceParameterNameDiscoverer(String pointcutExpression) {
		this.pointcutExpression = pointcutExpression;
	}
	
	/**
	 * <p>
	 * Set this property to true to indicate the IllegalArgumentException and AmbiguousBindingException
	 * should be thrown as appropriate in the case of failing to deduce advice parameter names.
	 * </p>
	 * @param shouldRaise
	 */
	public void setRaiseExceptions(boolean shouldRaise) {
		this.raiseExceptions = shouldRaise;
	}
	
	/**
	 * If afterReturning advice binds the return value, the returning 
	 * variable name must be specified.
	 * @param returningName
	 */
	public void setReturningName(String returningName) {
		this.returningName = returningName;
	}
	
	/**
	 * If afterThrowing advice binds the thrown value, the throwing 
	 * variable name must be specified.
	 * @param throwingName
	 */
	public void setThrowingName(String throwingName) {
		this.throwingName = throwingName;
	}
	
	/**
	 * <p>
	 * Deduce the parameter names for an advice method. See the javadoc comment for this class for 
	 * details of the algorithm used.
	 * </p>
	 */
	public String[] getParameterNames(Method method) {
		this.argumentTypes = method.getParameterTypes();
		this.numberOfRemainingUnboundArguments = this.argumentTypes.length;
		this.parameterNameBindings = new String[this.numberOfRemainingUnboundArguments];
		this.algorithmicStep = STEP_JOIN_POINT_BINDING;
		
		int minimumNumberUnboundArgs = 0;
		if (this.returningName != null) { minimumNumberUnboundArgs++; }
		if (this.throwingName != null) { minimumNumberUnboundArgs++; }
		if (this.numberOfRemainingUnboundArguments < minimumNumberUnboundArgs) {
			throw new IllegalStateException("Not enough arguments in method to satisfy binding of returning and throwing variables");
		}
		
		try {
			while ((this.numberOfRemainingUnboundArguments > 0) && (this.algorithmicStep < STEP_FINISHED)) {
				switch(this.algorithmicStep++) {
					case STEP_JOIN_POINT_BINDING:
						if (!maybeBindThisJoinPoint()) {
							maybeBindThisJoinPointStaticPart();
						}
						break;
					case STEP_THROWING_BINDING:
						maybeBindThrowingVariable();
						break;
					case STEP_ANNOTATION_BINDING:
						maybeBindAnnotationsFromPointcutExpression();
						break;
					case STEP_RETURNING_BINDING:
						maybeBindReturningVariable();
						break;
					case STEP_PRIMITIVE_ARGS_BINDING:
						maybeBindPrimitiveArgsFromPointcutExpression();
						break;
					case STEP_THIS_TARGET_ARGS_BINDING:
						maybeBindThisOrTargetOrArgsFromPointcutExpression();
						break;
					default:
						throw new IllegalStateException("Unknown algorithmic step: " + (this.algorithmicStep -1));
				}
			}
		} catch (AmbiguousBindingException ambigEx) {
			if (this.raiseExceptions) {
				throw ambigEx;
			} else {
				return null;
			}
		} catch (IllegalArgumentException illArgEx) {
			if (this.raiseExceptions) {
				throw illArgEx;
			} else {
				return null;
			}
		}
		
		if (this.numberOfRemainingUnboundArguments == 0) {
			return this.parameterNameBindings;
		} else {
			if (this.raiseExceptions) {
				throw new IllegalStateException("Failed to bind all argument names: " + 
						this.numberOfRemainingUnboundArguments + " argument(s) could not be bound");
			} else {
				// convention for failing is to return null, allowing participation in a chain of responsibility
				return null;
			}
		}
	}

	/**
	 * An advice method can never be a constructor.
	 * @return null
	 * @throws UnsupportedOperationException iff raiseExceptions has been set to true
	 */
	public String[] getParameterNames(Constructor ctor) {
		if (this.raiseExceptions) {
			throw new UnsupportedOperationException("An advice method can never be a constructor");
		} else {
			// we return null rather than throw an exception so that we behave well
			// in a chain-of-responsibility.
			return null;
		}
	}
	
	// support routines...
	// ==========================
	
	private void bindParameterName(int index, String name) {
		this.parameterNameBindings[index] = name;
		this.numberOfRemainingUnboundArguments--;
	}
	
	/**
	 * If the first parameter is of type JoinPoint or ProceedingJoinPoint,bind "thisJoinPoint" as 
	 * parameter name and return true, else return false. 
	 */
	private boolean maybeBindThisJoinPoint() {
		if ((this.argumentTypes[0] == JoinPoint.class) || (this.argumentTypes[0] == ProceedingJoinPoint.class)) {
			bindParameterName(0,THIS_JOIN_POINT);
			return true;
		} else {
			return false;
		}
	}
	
	private void maybeBindThisJoinPointStaticPart() {
		if (this.argumentTypes[0] == JoinPoint.StaticPart.class) {
			bindParameterName(0,THIS_JOIN_POINT_STATIC_PART);
		}
	}

	/**
	 * If a throwing name was specified and there is exactly one choice remaining
	 * (argument that is a subtype of Throwable) then bind it.
	 */
	private void maybeBindThrowingVariable() {
		if (this.throwingName == null) {
			return;
		}
		
		// so there is binding work to do...
		int throwableIndex = -1;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(Throwable.class,i)) {
				if (throwableIndex == -1) {
					throwableIndex = i;
				} else {
					// second candidate we've found - ambiguous binding
					throw new AmbiguousBindingException("Binding of throwing parameter '" + 
							this.throwingName + "' is ambiguous: could be bound to argument " +
							throwableIndex + " or argument " + i);
				}
			}
		}
		
		if (throwableIndex == -1) {
			throw new IllegalStateException("Binding of throwing parameter '" + this.throwingName 
					+ "' could not be completed as no available arguments are a subtype of Throwable");
		} else {
			bindParameterName(throwableIndex,this.throwingName);
		}
	}

	/**
	 * If a returning variable was specified and there is only one choice remaining, bind it.
	 */
	private void maybeBindReturningVariable() {
		if (this.numberOfRemainingUnboundArguments == 0) {
			throw new IllegalStateException("algorithm assumes that there must be at least one unbound parameter on entry to this method");
		}
		
		if (this.returningName != null) {
			if (this.numberOfRemainingUnboundArguments > 1) {
				throw new AmbiguousBindingException("Binding of returning parameter '" + 
						this.returningName + "' is ambiguous, there are " + this.numberOfRemainingUnboundArguments
						+ " candidates.");
			}
			
			// we're all set... find the unbound parameter, and bind it.
			for (int i = 0; i < this.parameterNameBindings.length; i++) {
				if (this.parameterNameBindings[i] == null) {
					bindParameterName(i,this.returningName);
					break;
				}
			}
		}
	}


	/**
	 * Parse the string pointcut expression looking for:
	 * &#64;this, &#64;target, &#64;args, &#64;within, &#64;withincode, &#64;annotation. If
	 * we find one of these pointcut expressions, try and extract a candidate variable name
	 * (or variable names, in the case of args).
	 *
	 * Some more support from AspectJ in doing this exercise would be nice...:)
	 */
	private void maybeBindAnnotationsFromPointcutExpression() {
		List varNames = new ArrayList();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression," ");
		for (int i = 0; i < tokens.length; i++) {
			String toMatch = tokens[i];
			int firstParenIndex = toMatch.indexOf("(");
			if (firstParenIndex != -1) {
				toMatch = toMatch.substring(0,firstParenIndex);
			}
			if (singleValuedAnnotationPcds.contains(toMatch)) {
				PointcutBody body = getPointcutBody(tokens,i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			} else if (tokens[i].startsWith("@args(") || tokens[i].equals("@args")) {
				PointcutBody body = getPointcutBody(tokens,i);
				i += body.numTokensConsumed;
				maybeExtractVariableNamesFromArgs(body.text,varNames);
			}
		}
		
		bindAnnotationsFromVarNames(varNames); 
	}

	/**
	 * Match the given list of extracted variable names to argument slots
	 * @param varNames
	 */
	private void bindAnnotationsFromVarNames(List varNames) {
		if (!varNames.isEmpty()) {
			// we have work to do...
			int numAnnotationSlots = countNumberOfUnboundAnnotationArguments();
			if (numAnnotationSlots > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() + 
						" potential annotation variable(s), and " +
						numAnnotationSlots + " potential argument slots");
			} else if (numAnnotationSlots == 1) {
				if (varNames.size() == 1) {
					// it's a match
					findAndBind(annotationClass,(String) varNames.get(0));
				} else {
					// multiple candidate vars, but only one slot
					throw new IllegalArgumentException("Found " + varNames.size() +
							" candidate annotation binding variables" + 
							" but only one potential argument binding slot" );
				}
			} else {
				// no slots so presume those candidate vars were actually type names
			}
		}
	}
	
	/**
	 * If the token starts meets Java identifier conventions, it's in. 
	 * @param candidateToken
	 * @return
	 */
	private String maybeExtractVariableName(String candidateToken) {
		if (candidateToken == null) { return null; }
		
		if (Character.isJavaIdentifierStart(candidateToken.charAt(0)) &&
			Character.isLowerCase(candidateToken.charAt(0))) {
			char[] tokenChars = candidateToken.toCharArray();
			for (int i = 0; i < tokenChars.length; i++) {
				if (!Character.isJavaIdentifierPart(tokenChars[i])) {
					return null;
				}
			}
			return candidateToken;
		} else {
			return null;
		}
	}
	
	/**
	 * Given an args pointcut body (could be args or at_args), add any
	 * candidate variable names to the given list.
	 */
	private void maybeExtractVariableNamesFromArgs(String argsSpec,List varNames) {
		if (argsSpec == null) { return; }
		
		String[] tokens = StringUtils.tokenizeToStringArray(argsSpec,",");
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = StringUtils.trimWhitespace(tokens[i]);
			String varName = maybeExtractVariableName(tokens[i]);
			if (varName != null) {
				varNames.add(varName);
			}
		}
	}

	/**
	 * Parse the string pointcut expression looking for this(), target() and args() expressions.
	 * If we find one, try and extract a candidate variable name and bind it.
	 */
	private void maybeBindThisOrTargetOrArgsFromPointcutExpression() {
		if (this.numberOfRemainingUnboundArguments > 1) {
			throw new AmbiguousBindingException("Still " + this.numberOfRemainingUnboundArguments 
					+ " unbound args at this(),target(),args() binding stage, with no way to determine between them"); 
		}
		
		List varNames = new ArrayList();
		String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression," ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("this") ||
				tokens[i].startsWith("this(") ||
				tokens[i].equals("target") ||
				tokens[i].startsWith("target(")) {
				PointcutBody body = getPointcutBody(tokens,i);
				i += body.numTokensConsumed;
				String varName = maybeExtractVariableName(body.text);
				if (varName != null) {
					varNames.add(varName);
				}
			} else if (tokens[i].equals("args") || tokens[i].startsWith("args(") ) {
				PointcutBody body = getPointcutBody(tokens,i);
				i += body.numTokensConsumed;
				List candidateVarNames = new ArrayList();
				maybeExtractVariableNamesFromArgs(body.text,candidateVarNames);				
				// we may have found some var names that were bound in previous primitive args binding step,
				// filter them out...
				for (Iterator iter = candidateVarNames.iterator(); iter.hasNext();) {
					String varName = (String) iter.next();
					if (!alreadyBound(varName)) {
						varNames.add(varName);
					}
				}
			}
		}
		
		
		if (varNames.size() > 1) {
			throw new AmbiguousBindingException("Found " + varNames.size() + 
					" candidate this(), target() or args() variables but only one unbound argument slot");
		} else if (varNames.size() == 1) {
			for (int j = 0; j < this.parameterNameBindings.length; j++) {
				if (isUnbound(j)) {
					bindParameterName(j,(String)varNames.get(0));
					break;
				}
			}
		}
		// else varNames.size must be 0 and we have nothing to bind.
	}

	/**
	 * We've found the start of a binding pointcut at the given index into 
	 * the token array. Now we need to extract the pointcut body and return
	 * it.
	 * @param tokens
	 * @param startIndex
	 * @return
	 */
	private PointcutBody getPointcutBody(String[] tokens, int startIndex) {
		int numTokensConsumed = 0;
		String currentToken = tokens[startIndex];
		int bodyStart = currentToken.indexOf('(');
		if (currentToken.charAt(currentToken.length() - 1) == ')') {
			// it's an all in one... get the text between the first ( and the last )
			return new PointcutBody(0,currentToken.substring(bodyStart + 1,currentToken.length() - 1));
		} else {
			StringBuffer sb = new StringBuffer();
			if (bodyStart >= 0 && bodyStart != (currentToken.length() - 1)) {
				sb.append(currentToken.substring(bodyStart + 1));
				sb.append(" ");
			}
			numTokensConsumed++;
			int currentIndex = startIndex + numTokensConsumed;
			while( currentIndex < tokens.length) {
				if (tokens[currentIndex].equals("(")) {
					continue;
				}
				
				if (tokens[currentIndex].endsWith(")")) {
					sb.append(tokens[currentIndex].substring(0,tokens[currentIndex].length() - 1));
					return new PointcutBody(numTokensConsumed,sb.toString().trim());
				}

				String toAppend = tokens[currentIndex];
				if (toAppend.startsWith("(")) {
					toAppend = toAppend.substring(1);
				}
				sb.append(toAppend);
				sb.append(" ");			     
				currentIndex++;
				numTokensConsumed++;
			}
			
		}
		
		// we looked and failed...
		return new PointcutBody(numTokensConsumed,null);
	}
	
	/**
	 * match up args against unbound arguments of primitive types
	 */
	private void maybeBindPrimitiveArgsFromPointcutExpression() {
		int numUnboundPrimitives = countNumberOfUnboundPrimitiveArguments();
		if (numUnboundPrimitives > 1) {
			throw new AmbiguousBindingException("Found '" + numUnboundPrimitives + 
					"' unbound primitive arguments with no way to distinguish between them.");
		}
		
		if (numUnboundPrimitives == 1) {
			// look for arg variable and bind it if we find exactly one
			List varNames = new ArrayList();
			String[] tokens = StringUtils.tokenizeToStringArray(this.pointcutExpression," ");
			for (int i = 0; i < tokens.length; i++) {
				if (tokens[i].equals("args") || tokens[i].startsWith("args(") ) {
					PointcutBody body = getPointcutBody(tokens,i);
					i += body.numTokensConsumed;				
					maybeExtractVariableNamesFromArgs(body.text,varNames);				
				}
			}	
			if (varNames.size() > 1) {
				throw new AmbiguousBindingException("Found " + varNames.size() + 
						" candidate variable names but only one candidate binding slot when matching primitive args");
			} else if (varNames.size() == 0) {
				throw new IllegalArgumentException("Unable to find args match for unbound primitive argument");
			} else {
				// 1 primitive arg, and one candidate...
				for (int i = 0; i < this.argumentTypes.length; i++) {
					if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
						bindParameterName(i,(String) varNames.get(0));
						break;
					}
				}
			}
		}
		
	}

	/**
	 * true if the parameter name binding for the given parameter index has not yet been assigned
	 * @param i
	 * @return
	 */
	private boolean isUnbound(int i) {
		return this.parameterNameBindings[i] == null;
	}
	
	private boolean alreadyBound(String varName) {
		for (int i = 0; i < this.parameterNameBindings.length; i++) {
			if (!isUnbound(i) && varName.equals(this.parameterNameBindings[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * true if the given argument type is a subclass of the given supertype
	 * @param supertype
	 * @param argumentNumber
	 * @return
	 */
	private boolean isSubtypeOf(Class supertype, int argumentNumber) {
		return supertype.isAssignableFrom(this.argumentTypes[argumentNumber]);
	}
	
	private int countNumberOfUnboundAnnotationArguments() {
		if (annotationClass == null) {
			// we're running on a JDK < 1.5
			return 0;
		}
		
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(annotationClass,i)) {
				count++;
			}			
		}
		return count;
	}
	
	private int countNumberOfUnboundPrimitiveArguments() {
		int count = 0;
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && this.argumentTypes[i].isPrimitive()) {
				count++;
			}			
		}
		return count;
	}
	
	/**
	 * find the argument index with the given type, and bind the
	 * given varName in that position
	 * @param argumentType
	 * @param varName
	 */
	private void findAndBind(Class argumentType, String varName) {
		for (int i = 0; i < this.argumentTypes.length; i++) {
			if (isUnbound(i) && isSubtypeOf(argumentType,i)) {
				bindParameterName(i,varName);
				return;
			}
		}
		throw new IllegalStateException("Expected to find an unbound argument of type '" +
				argumentType.getName() + "'");
	}
	
	/**
	 * Simple struct to hold the extracted text from a pointcut body, together
	 * with the number of tokens consumed in extracting it.
	 */
	private static class PointcutBody {
		int numTokensConsumed = 0;
		String text = null;
		public PointcutBody(int tokens, String text) {
			this.numTokensConsumed = tokens;
			this.text = text;
		}
	}


	public static class AmbiguousBindingException extends RuntimeException {

		public AmbiguousBindingException(String explanation) {
			super(explanation);
		}
	}

}
