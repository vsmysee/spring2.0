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

package org.springframework.jmx.support;

import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;

import javax.management.DynamicMBean;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.JdkVersion;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Collection of generic utility methods to support Spring JMX.
 * Includes a convenient method to locate an MBeanServer.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 */
public abstract class JmxUtils {

	private static final Log logger = LogFactory.getLog(JmxUtils.class);

	/**
	 * Suffix used to identify an MBean interface
	 */
	private static final String MBEAN_SUFFIX = "MBean";

	/**
	 * The key used when extending an existing {@link javax.management.ObjectName} with the
	 * identity hash code of its corresponding managed resource.
	 */
	public static final String IDENTITY_OBJECT_NAME_KEY = "identity";


	/**
	 * Attempt to find a locally running <code>MBeanServer</code>. Fails if no
	 * <code>MBeanServer</code> can be found. Logs a warning if more than one
	 * <code>MBeanServer</code> found, returning the first one from the list.
	 * @return the <code>MBeanServer</code> if found
	 * @throws org.springframework.jmx.MBeanServerNotFoundException
	 * if no <code>MBeanServer</code> could be found
	 * @see javax.management.MBeanServerFactory#findMBeanServer
	 */
	public static MBeanServer locateMBeanServer() throws MBeanServerNotFoundException {
		return locateMBeanServer(null);
	}

	/**
	 * Attempt to find a locally running <code>MBeanServer</code>. Fails if no
	 * <code>MBeanServer</code> can be found. Logs a warning if more than one
	 * <code>MBeanServer</code> found, returning the first one from the list.
	 * @param agentId the agent identifier of the MBeanServer to retrieve.
	 * If this parameter is <code>null</code>, all registered MBeanServers are
	 * considered.
	 * @return the <code>MBeanServer</code> if found
	 * @throws org.springframework.jmx.MBeanServerNotFoundException
	 * if no <code>MBeanServer</code> could be found
	 * @see javax.management.MBeanServerFactory#findMBeanServer(String)
	 */
	public static MBeanServer locateMBeanServer(String agentId) throws MBeanServerNotFoundException {
		List servers = MBeanServerFactory.findMBeanServer(agentId);

		MBeanServer server = null;
		if (servers != null && servers.size() > 0) {
			// Check to see if an MBeanServer is registered.
			if (servers.size() > 1 && logger.isWarnEnabled()) {
				logger.warn("Found more than one MBeanServer instance" +
						(agentId != null ? " with agent id [" + agentId + "]" : "") +
						". Returning first from list.");
			}
			server = (MBeanServer) servers.get(0);
		}
		else if (JdkVersion.isAtLeastJava15()) {
			// Attempt to load the PlatformMBeanServer.
			server = ManagementFactory.getPlatformMBeanServer();
		}

		if (server == null) {
			throw new MBeanServerNotFoundException(
					"Unable to locate an MBeanServer instance" +
					(agentId != null ? " with agent id [" + agentId + "]" : ""));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found MBeanServer: " + server);
		}
		return server;
	}

	/**
	 * Convert an array of <code>MBeanParameterInfo</code> into an array of
	 * <code>Class</code> instances corresponding to the parameters.
	 */
	public static Class[] parameterInfoToTypes(MBeanParameterInfo[] paramInfo) throws ClassNotFoundException {
		Class[] types = null;
		if (paramInfo != null && paramInfo.length > 0) {
			types = new Class[paramInfo.length];
			for (int x = 0; x < paramInfo.length; x++) {
				types[x] = ClassUtils.forName(paramInfo[x].getType());
			}
		}
		return types;
	}

	/**
	 * Create a <code>String[]</code> representing the signature of a method.
	 * Each element in the array is the fully qualified class name
	 * of the corresponding argument in the methods signature.
	 */
	public static String[] getMethodSignature(Method method) {
		Class[] types = method.getParameterTypes();
		String[] signature = new String[types.length];
		for (int x = 0; x < types.length; x++) {
			signature[x] = types[x].getName();
		}
		return signature;
	}

	/**
	 * Return the JMX attribute name to use for the given JavaBeans property.
	 * <p>When using strict casing, a JavaBean property with a getter method
	 * such as <code>getFoo()</code> translates to an attribute called
	 * <code>Foo</code>. With strict casing disabled, <code>getFoo()</code>
	 * would translate to just <code>foo</code>.
	 * @param property the JavaBeans property descriptor
	 * @param useStrictCasing whether to use strict casing
	 * @return the JMX attribute name to use
	 */
	public static String getAttributeName(PropertyDescriptor property, boolean useStrictCasing) {
		if (useStrictCasing) {
			return StringUtils.capitalize(property.getName());
		}
		else {
			return property.getName();
		}
	}

	/**
	 * Check whether the supplied <code>Class</code> is a valid MBean resource.
	 * @param beanClass the class of the bean to test
	 */
	public static boolean isMBean(Class beanClass) {
		if (beanClass == null) {
			return false;
		}
		if (DynamicMBean.class.isAssignableFrom(beanClass)) {
			return true;
		}
		Class cls = beanClass;
		while (cls != null && cls != Object.class) {
			if (hasMBeanInterface(cls)) {
				return true;
			}
			cls = cls.getSuperclass();
		}
		return false;
	}

	/**
	 * Return the class or interface to expose for the given bean.
	 * This is the class that will be searched for attributes and operations
	 * (for example, checked for annotations).
	 * <p>Default implementation returns the target class for a CGLIB proxy,
	 * and the class of the given bean else (for a JDK proxy or a plain bean class).
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @return the bean class to expose
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 * @see org.springframework.aop.framework.Advised#getTargetSource()
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public static Class getClassToExpose(Object managedBean) {
		if (AopUtils.isCglibProxy(managedBean)) {
			return managedBean.getClass().getSuperclass();
		}
		return managedBean.getClass();
	}

	/**
	 * Return the class or interface to expose for the given bean class.
	 * This is the class that will be searched for attributes and operations
	 * (for example, checked for annotations).
	 * <p>Default implementation returns the superclass for a CGLIB proxy,
	 * and the given bean class else (for a JDK proxy or a plain bean class).
	 * @param beanClass the bean class (might be an AOP proxy class)
	 * @return the bean class to expose
	 * @see org.springframework.aop.support.AopUtils#isCglibProxyClass(Class)
	 */
	public static Class getClassToExpose(Class beanClass) {
		if (AopUtils.isCglibProxyClass(beanClass)) {
			return beanClass.getSuperclass();
		}
		return beanClass;
	}

	/**
	 * Return whether an MBean interface exists for the given class
	 * (that is, an interface whose name matches the class name of
	 * the given class but with suffix "MBean).
	 * @param clazz the class to check
	 */
	private static boolean hasMBeanInterface(Class clazz) {
		Class[] implementedInterfaces = clazz.getInterfaces();
		String mbeanInterfaceName = clazz.getName() + MBEAN_SUFFIX;
		for (int x = 0; x < implementedInterfaces.length; x++) {
			Class iface = implementedInterfaces[x];
			if (iface.getName().equals(mbeanInterfaceName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Append an additional key/value pair to an existing {@link javax.management.ObjectName} with the key being
	 * the static value <code>identity</code> and the value being the identity hash code of the
	 * managed resource being exposed on the supplied {@link javax.management.ObjectName}. This can be used to
	 * provide a unique {@link javax.management.ObjectName} for each distinct instance of a particular bean or
	 * class. Useful when generating {@link javax.management.ObjectName ObjectNames} at runtime for a set of
	 * managed resources based on the template value supplied by a
	 * {@link org.springframework.jmx.export.naming.ObjectNamingStrategy}.
	 */
	public static ObjectName appendIdentityToObjectName(ObjectName objectName, Object managedResource)
			throws MalformedObjectNameException {

		Hashtable keyProperties = objectName.getKeyPropertyList();
		keyProperties.put(IDENTITY_OBJECT_NAME_KEY, ObjectUtils.getIdentityHexString(managedResource));
		return ObjectNameManager.getInstance(objectName.getDomain(), keyProperties);
	}

}
