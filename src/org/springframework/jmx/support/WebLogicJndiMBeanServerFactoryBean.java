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

package org.springframework.jmx.support;

import java.lang.reflect.InvocationTargetException;

import javax.management.MBeanServer;
import javax.naming.NamingException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.ClassUtils;

/**
 * FactoryBean that obtains the WebLogic <code>MBeanServer</code> instance
 * through a WebLogic <code>MBeanHome</code> obtained via a JNDI lookup.
 * By default, the server's local <code>MBeanHome</code> will be obtained.
 *
 * <p>Exposes the <code>MBeanServer</code> for bean references.
 * This FactoryBean is a direct alternative to <code>MBeanServerFactoryBean</code>,
 * which uses standard JMX 1.2 API to access the platform's MBeanServer.
 *
 * <p>Note: There is also a more general <code>WebLogicMBeanServerFactoryBean</code>
 * for accessing any specified WebLogic <code>MBeanServer</code>, potentially a
 * remote one.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see weblogic.management.MBeanHome#LOCAL_JNDI_NAME
 * @see weblogic.management.MBeanHome#getMBeanServer()
 * @see org.springframework.jmx.support.MBeanServerFactoryBean
 * @see WebLogicMBeanServerFactoryBean
 */
public class WebLogicJndiMBeanServerFactoryBean extends JndiLocatorSupport
		implements FactoryBean, InitializingBean {

	private static final String WEBLOGIC_MBEAN_HOME_CLASS = "weblogic.management.MBeanHome";

	private static final String LOCAL_JNDI_NAME_FIELD = "LOCAL_JNDI_NAME";

	private static final String GET_MBEAN_SERVER_METHOD = "getMBeanServer";


	private String mbeanHomeName;

	private MBeanServer mbeanServer;


	/**
	 * Specify the JNDI name of the WebLogic MBeanHome object to use
	 * for creating the JMX MBeanServer reference.
	 * <p>Default is <code>MBeanHome.LOCAL_JNDI_NAME</code>
	 * @see weblogic.management.MBeanHome#LOCAL_JNDI_NAME
	 */
	public void setMbeanHomeName(String mbeanHomeName) {
		this.mbeanHomeName = mbeanHomeName;
	}


	public void afterPropertiesSet() throws MBeanServerNotFoundException {
		try {
			String jndiName = this.mbeanHomeName;
			if (jndiName == null) {
				/*
				 * jndiName = MBeanHome.LOCAL_JNDI_NAME;
				 */
				Class mbeanHomeClass = ClassUtils.forName(WEBLOGIC_MBEAN_HOME_CLASS);
				jndiName = (String) mbeanHomeClass.getField(LOCAL_JNDI_NAME_FIELD).get(null);
			}
			Object mbeanHome = lookup(jndiName);

			/*
			 * this.mbeanServer = mbeanHome.getMBeanServer();
			 */
			this.mbeanServer = (MBeanServer)
					mbeanHome.getClass().getMethod(GET_MBEAN_SERVER_METHOD, null).invoke(mbeanHome, null);
		}
		catch (NamingException ex) {
			throw new MBeanServerNotFoundException("Could not find WebLogic's MBeanHome object in JNDI", ex);
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanServerNotFoundException("Could not find WebLogic's MBeanHome class", ex);
		}
		catch (InvocationTargetException ex) {
			throw new MBeanServerNotFoundException(
					"WebLogic's MBeanHome.getMBeanServer method failed", ex.getTargetException());
		}
		catch (Exception ex) {
			throw new MBeanServerNotFoundException(
					"Could not access WebLogic's MBeanHome/getMBeanServer method", ex);
		}
	}


	public Object getObject() {
		return this.mbeanServer;
	}

	public Class getObjectType() {
		return (this.mbeanServer != null ? this.mbeanServer.getClass() : MBeanServer.class);
	}

	public boolean isSingleton() {
		return true;
	}

}
