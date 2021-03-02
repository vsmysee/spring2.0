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

package org.springframework.jms.support;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.jms.JmsException;

/**
 * Base class for {@link org.springframework.jms.core.JmsTemplate} and other
 * JMS-accessing gateway helpers, defining common properties like the
 * {@link javax.jms.ConnectionFactory}. The subclass
 * {@link org.springframework.jms.support.destination.JmsDestinationAccessor} adds
 * further, destination-related properties.
 *
 * <p>Not intended to be used directly. See {@link org.springframework.jms.core.JmsTemplate}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see org.springframework.jms.support.destination.JmsDestinationAccessor
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsAccessor implements InitializingBean {

	/** Constants instance for javax.jms.Session */
	private static final Constants sessionConstants = new Constants(Session.class);


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory connectionFactory;

	private boolean sessionTransacted = false;

	private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;


	/**
	 * Set the ConnectionFactory to use for obtaining JMS Connections.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the ConnectionFactory to use for obtaining JMS Connections.
	 */
	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	/**
	 * Set the transaction mode that is used when creating a JMS {@link javax.jms.Session}.
	 * Default is "false".
	 * <p>Note that that within a JTA transaction, the parameters to
	 * <code>create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)</code>
	 * method are not taken into account. Depending on the J2EE transaction context,
	 * the container makes its own decisions on these values. See section 17.3.5
	 * of the EJB specification. Analogously, these parameters are not taken into
	 * account within a locally managed transaction either, since the accessor
	 * operates on an existing JMS Session in this case.
	 * <p>Setting this flag to "true" will use a short local JMS transaction
	 * when running outside of a managed transaction, and a synchronized local
	 * JMS transaction in case of a managed transaction (other than an XA
	 * transaction) being present. The latter has the effect of a local JMS
	 * transaction being managed alongside the main transaction (which might
	 * be a native JDBC transaction), with the JMS transaction committing
	 * right after the main transaction.
	 * @param sessionTransacted the transaction mode
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	/**
	 * Return whether the JMS {@link javax.jms.Session sessions} used by this
	 * accessor are supposed to be transacted.
	 * @return <code>true</code> if the JMS Sessions used
	 * for sending a message are transacted
	 * @see #setSessionTransacted(boolean)
	 */
	public boolean isSessionTransacted() {
		return sessionTransacted;
	}

	/**
	 * Set the JMS acknowledgement mode by the name of the corresponding constant
	 * in the JMS {@link javax.jms.Session} interface, e.g. "CLIENT_ACKNOWLEDGE".
	 * <p>If you want to use vendor-specific extensions to the acknowledgment mode,
	 * use {@link #setSessionAcknowledgeModeName(String)} instead.
	 * @param constantName the name of the {@link javax.jms.Session} acknowledge mode constant
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeModeName(String constantName) {
		setSessionAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * Set the JMS acknowledgement mode that is used when creating a JMS
	 * {@link javax.jms.Session} to send a message.
	 * <p>Default is {@link javax.jms.Session#AUTO_ACKNOWLEDGE}.
	 * <p>Vendor-specific extensions to the acknowledgment mode can be set here as well.
	 * <p>Note that that inside an EJB the parameters to
	 * create(Queue/Topic)Session(boolean transacted, int acknowledgeMode) method
	 * are not taken into account. Depending on the transaction context in the EJB,
	 * the container makes its own decisions on these values. See section 17.3.5
	 * of the EJB spec.
	 * @param sessionAcknowledgeMode the acknowledgement mode
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Connection#createSession(boolean, int)
	 */
	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * Return the acknowledgement mode for JMS {@link javax.jms.Session sessions}.
	 */
	public int getSessionAcknowledgeMode() {
		return sessionAcknowledgeMode;
	}

	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("connectionFactory is required");
		}
	}


	/**
	 * Convert the specified checked {@link javax.jms.JMSException JMSException} to
	 * a Spring runtime {@link org.springframework.jms.JmsException JmsException}
	 * equivalent.
	 * <p>Default implementation delegates to the
	 * {@link org.springframework.jms.support.JmsUtils#convertJmsAccessException} method.
	 * @param ex the original checked {@link javax.jms.JMSException} to convert
	 * @return the Spring runtime {@link org.springframework.jms.JmsException} wrapping <code>ex</code>
	 * @see org.springframework.jms.support.JmsUtils#convertJmsAccessException
	 */
	protected JmsException convertJmsAccessException(JMSException ex) {
		return JmsUtils.convertJmsAccessException(ex);
	}

}
