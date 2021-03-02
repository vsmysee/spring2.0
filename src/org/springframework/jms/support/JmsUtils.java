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

import java.lang.reflect.Constructor;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueRequestor;
import javax.jms.Session;
import javax.jms.TransactionInProgressException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.JmsException;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Generic utility methods for working with JMS. Mainly for internal use
 * within the framework, but also useful for custom JMS access code.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public abstract class JmsUtils {

	private static final Log logger = LogFactory.getLog(JmsUtils.class);


	/**
	 * Close the given JMS Connection and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param con the JMS Connection to close (may be <code>null</code>)
	 */
	public static void closeConnection(Connection con) {
		closeConnection(con, false);
	}

	/**
	 * Close the given JMS Connection and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param con the JMS Connection to close (may be <code>null</code>)
	 * @param stop whether to call <code>stop()</code> before closing
	 */
	public static void closeConnection(Connection con, boolean stop) {
		if (con != null) {
			try {
				if (stop) {
					try {
						con.stop();
					}
					finally {
						con.close();
					}
				}
				else {
					con.close();
				}
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS Connection", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS Connection", ex);
			}
		}
	}

	/**
	 * Close the given JMS Session and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param session the JMS Session to close (may be <code>null</code>)
	 */
	public static void closeSession(Session session) {
		if (session != null) {
			try {
				session.close();
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS Session", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS Session", ex);
			}
		}
	}

	/**
	 * Close the given JMS MessageProducer and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param producer the JMS MessageProducer to close (may be <code>null</code>)
	 */
	public static void closeMessageProducer(MessageProducer producer) {
		if (producer != null) {
			try {
				producer.close();
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS MessageProducer", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS MessageProducer", ex);
			}
		}
	}

	/**
	 * Close the given JMS MessageConsumer and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param consumer the JMS MessageConsumer to close (may be <code>null</code>)
	 */
	public static void closeMessageConsumer(MessageConsumer consumer) {
		if (consumer != null) {
			try {
				consumer.close();
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS MessageConsumer", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS MessageConsumer", ex);
			}
		}
	}

	/**
	 * Close the given JMS QueueRequestor and ignore any thrown exception.
	 * This is useful for typical <code>finally</code> blocks in manual JMS code.
	 * @param requestor the JMS QueueRequestor to close (may be <code>null</code>)
	 */
	public static void closeQueueRequestor(QueueRequestor requestor) {
		if (requestor != null) {
			try {
				requestor.close();
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS QueueRequestor", ex);
			}
			catch (Throwable ex) {
				// We don't trust the JMS provider: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JMS QueueRequestor", ex);
			}
		}
	}

	/**
	 * Commit the Session if not within a JTA transaction.
	 * @param session the JMS Session to commit
	 * @throws javax.jms.JMSException if committing failed
	 */
	public static void commitIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.commit();
		}
		catch (TransactionInProgressException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
		catch (javax.jms.IllegalStateException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
	}

	/**
	 * Rollback the Session if not within a JTA transaction.
	 * @param session the JMS Session to rollback
	 * @throws javax.jms.JMSException if committing failed
	 */
	public static void rollbackIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.rollback();
		}
		catch (TransactionInProgressException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
		catch (javax.jms.IllegalStateException ex) {
			// Ignore -> can only happen in case of a JTA transaction.
		}
	}

	/**
	 * Convert the specified checked {@link javax.jms.JMSException JMSException} to
	 * a Spring runtime {@link org.springframework.jms.JmsException JmsException}
	 * equivalent.
	 * @param ex the original checked JMSException to convert
	 * @return the Spring runtime JmsException wrapping <code>ex</code>.
	 */
	public static JmsException convertJmsAccessException(JMSException ex) {
		Assert.notNull(ex, "JMSException must not be null");

		if (ex instanceof JMSSecurityException) {
			return new JmsSecurityException((JMSSecurityException) ex);
		}

		if (JMSException.class.equals(ex.getClass().getSuperclass())) {
			// All other exceptions in our Jms runtime exception hierarchy have the
			// same unqualified names as their javax.jms counterparts, so just
			// construct the converted exception dynamically based on name.
			String shortName = ClassUtils.getShortName(ex.getClass());

			// All JmsException subclasses reside in the same package.
			String longName = JmsException.class.getPackage().getName() + "." + shortName;

			try {
				Class clazz = ClassUtils.forName(longName, JmsException.class.getClassLoader());
				Constructor ctor = clazz.getConstructor(new Class[] {ex.getClass()});
				Object counterpart = ctor.newInstance(new Object[] {ex});
				return (JmsException) counterpart;
			}
			catch (Throwable ex2) {
				if (logger.isDebugEnabled()) {
					logger.debug("Couldn't resolve JmsException class [" + longName + "]", ex2);
				}
				return new UncategorizedJmsException(ex);
			}
		}

		// Fallback: uncategorized exception.
		return new UncategorizedJmsException(ex);
	}

}
