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

package org.springframework.jms.connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A JMS ConnectionFactory adapter that returns the same Connection on all
 * <code>createConnection</code> calls, and ignores calls to
 * <code>Connection.close()</code>. According to the JMS Connection model,
 * this is even thread-safe.
 *
 * <p>Useful for testing and standalone environments, to keep using the same
 * Connection for multiple JmsTemplate calls, without having a pooling
 * ConnectionFactory, also spanning any number of transactions.
 *
 * <p>You can either pass in a JMS Connection directly, or let this
 * factory lazily create a Connection via a given target ConnectionFactory.
 * In the latter case, this factory just works with JMS 1.1; use
 * SingleConnectionFactory102 for JMS 1.0.2.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see #createConnection()
 * @see javax.jms.Connection#close()
 * @see org.springframework.jms.core.JmsTemplate
 * @see SingleConnectionFactory102
 */
public class SingleConnectionFactory
		implements ConnectionFactory, QueueConnectionFactory, TopicConnectionFactory, ExceptionListener,
		InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory targetConnectionFactory;

	private String clientId;

	private ExceptionListener exceptionListener;

	private boolean reconnectOnException = false;

	/** Wrapped Connection */
	private Connection target;

	/** Proxy Connection */
	private Connection connection;

	/** Synchronization monitor for the shared Connection */
	private final Object connectionMonitor = new Object();


	/**
	 * Create a new SingleConnectionFactory for bean-style usage.
	 * @see #setTargetConnectionFactory
	 */
	public SingleConnectionFactory() {
	}

	/**
	 * Create a new SingleConnectionFactory that always returns the
	 * given Connection. Works with both JMS 1.1 and 1.0.2.
	 * @param target the single Connection
	 */
	public SingleConnectionFactory(Connection target) {
		Assert.notNull(target, "Target Connection must not be null");
		this.target = target;
		this.connection = getSharedConnectionProxy(target);
	}

	/**
	 * Create a new SingleConnectionFactory that always returns a single
	 * Connection that it will lazily create via the given target
	 * ConnectionFactory.
	 * @param targetConnectionFactory the target ConnectionFactory
	 */
	public SingleConnectionFactory(ConnectionFactory targetConnectionFactory) {
		Assert.notNull(targetConnectionFactory, "Target ConnectionFactory must not be null");
		this.targetConnectionFactory = targetConnectionFactory;
	}


	/**
	 * Set the target ConnectionFactory which will be used to lazily
	 * create a single Connection.
	 */
	public void setTargetConnectionFactory(ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory which will be used to lazily
	 * create a single Connection, if any.
	 */
	public ConnectionFactory getTargetConnectionFactory() {
		return targetConnectionFactory;
	}

	/**
	 * Specify a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory.
	 * <p>Note that client IDs need to be unique among all active Connections
	 * of the underlying JMS provider. Furthermore, a client ID can only be
	 * assigned if the original ConnectionFactory hasn't already assigned one.
	 * @see javax.jms.Connection#setClientID
	 * @see #setTargetConnectionFactory
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Return a JMS client ID for the single Connection created and exposed
	 * by this ConnectionFactory, if any.
	 */
	protected String getClientId() {
		return clientId;
	}

	/**
	 * Specify an JMS ExceptionListener implementation that should be
	 * registered with with the single Connection created by this factory.
	 * @see #setReconnectOnException
	 */
	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * Return the JMS ExceptionListener implementation that should be registered
	 * with with the single Connection created by this factory, if any.
	 */
	protected ExceptionListener getExceptionListener() {
		return exceptionListener;
	}

	/**
	 * Specify whether the single Connection should be reset (to be subsequently renewed)
	 * when a JMSException is reported by the underlying Connection.
	 * <p>Default is "false". Switch this to "true" to automatically trigger
	 * recovery based on your JMS provider's exception notifications.
	 * <p>Internally, this will lead to a special JMS ExceptionListener
	 * (this SingleConnectionFactory itself) being registered with the
	 * underlying Connection. This can also be combined with a
	 * user-specified ExceptionListener, if desired.
	 * @see #setExceptionListener
	 */
	public void setReconnectOnException(boolean reconnectOnException) {
		this.reconnectOnException = reconnectOnException;
	}

	/**
	 * Return whether the single Connection should be renewed when
	 * a JMSException is reported by the underlying Connection.
	 */
	protected boolean isReconnectOnException() {
		return reconnectOnException;
	}

	/**
	 * Make sure a Connection or ConnectionFactory has been set.
	 */
	public void afterPropertiesSet() {
		if (this.connection == null && getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Connection or targetConnectionFactory is required");
		}
	}


	public Connection createConnection() throws JMSException {
		synchronized (this.connectionMonitor) {
			if (this.connection == null) {
				initConnection();
			}
			return this.connection;
		}
	}

	public Connection createConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	public QueueConnection createQueueConnection() throws JMSException {
		Connection con = createConnection();
		if (!(con instanceof QueueConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a QueueConnection but rather: " + con);
		}
		return ((QueueConnection) con);
	}

	public QueueConnection createQueueConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	public TopicConnection createTopicConnection() throws JMSException {
		Connection con = createConnection();
		if (!(con instanceof TopicConnection)) {
			throw new javax.jms.IllegalStateException(
					"This SingleConnectionFactory does not hold a TopicConnection but rather: " + con);
		}
		return ((TopicConnection) con);
	}

	public TopicConnection createTopicConnection(String username, String password) throws JMSException {
		throw new javax.jms.IllegalStateException(
				"SingleConnectionFactory does not support custom username and password");
	}

	/**
	 * Exception listener callback that renews the underlying single Connection.
	 */
	public void onException(JMSException ex) {
		resetConnection();
	}

	/**
	 * Close the underlying shared connection.
	 * The provider of this ConnectionFactory needs to care for proper shutdown.
	 * <p>As this bean implements DisposableBean, a bean factory will
	 * automatically invoke this on destruction of its cached singletons.
	 */
	public void destroy() {
		resetConnection();
	}


	/**
	 * Initialize the underlying shared Connection.
	 * <p>Closes and reinitializes the Connection if an underlying
	 * Connection is present already.
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	public void initConnection() throws JMSException {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalStateException("targetConnectionFactory is required for lazily initializing a Connection");
		}
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = doCreateConnection();
			prepareConnection(this.target);
			if (logger.isInfoEnabled()) {
				logger.info("Established shared JMS Connection: " + this.target);
			}
			this.connection = getSharedConnectionProxy(this.target);
		}
	}

	/**
	 * Reset the underlying shared Connection, to be reinitialized on next access.
	 */
	public void resetConnection() {
		synchronized (this.connectionMonitor) {
			if (this.target != null) {
				closeConnection(this.target);
			}
			this.target = null;
			this.connection = null;
		}
	}

	/**
	 * Create a JMS Connection via this template's ConnectionFactory.
	 * <p>This implementation uses JMS 1.1 API.
	 * @return the new JMS Connection
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected Connection doCreateConnection() throws JMSException {
		return getTargetConnectionFactory().createConnection();
	}

	/**
	 * Prepare the given Connection before it is exposed.
	 * <p>The default implementation applies ExceptionListener and client id.
	 * Can be overridden in subclasses.
	 * @param con the Connection to prepare
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 * @see #setExceptionListener
	 * @see #setReconnectOnException
	 */
	protected void prepareConnection(Connection con) throws JMSException {
		if (getExceptionListener() != null || isReconnectOnException()) {
			ExceptionListener listenerToUse = getExceptionListener();
			if (isReconnectOnException()) {
				listenerToUse = new InternalChainedExceptionListener(this, listenerToUse);
			}
			con.setExceptionListener(listenerToUse);
		}
		if (getClientId() != null) {
			con.setClientID(getClientId());
		}
	}

	/**
	 * Close the given Connection.
	 * @param con the Connection to close
	 */
	protected void closeConnection(Connection con) {
		try {
			try {
				con.stop();
			}
			finally {
				con.close();
			}
		}
		catch (Throwable ex) {
			logger.warn("Could not close shared JMS Connection", ex);
		}
	}

	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it
	 * but suppresses close calls. This is useful for allowing application code to
	 * handle a special framework Connection just like an ordinary Connection from a
	 * JMS ConnectionFactory.
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getSharedConnectionProxy(Connection target) {
		List classes = new ArrayList(3);
		classes.add(Connection.class);
		if (target instanceof QueueConnection) {
			classes.add(QueueConnection.class);
		}
		if (target instanceof TopicConnection) {
			classes.add(TopicConnection.class);
		}
		return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				(Class[]) classes.toArray(new Class[classes.size()]),
				new SharedConnectionInvocationHandler(target));
	}


	/**
	 * Invocation handler that suppresses close calls on JMS Connections.
	 */
	private static class SharedConnectionInvocationHandler implements InvocationHandler {

		private final Connection target;

		private SharedConnectionInvocationHandler(Connection target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of Connection proxy.
				return new Integer(hashCode());
			}
			else if (method.getName().equals("setExceptionListener")) {
				// Handle setExceptionListener method: throw exception.
				throw new javax.jms.IllegalStateException(
						"setExceptionListener call not supported on proxy for shared Connection. " +
						"Set the 'exceptionListener' property on the SingleConnectionFactory instead.");
			}
			else if (method.getName().equals("setClientID")) {
				// Handle setExceptionListener method: throw exception.
				throw new javax.jms.IllegalStateException(
						"setClientID call not supported on proxy for shared Connection. " +
						"Set the 'clientId' property on the SingleConnectionFactory instead.");
			}
			else if (method.getName().equals("stop")) {
				// Handle stop method: don't pass the call on.
				return null;
			}
			else if (method.getName().equals("close")) {
				// Handle close method: don't pass the call on.
				return null;
			}
			try {
				Object retVal = method.invoke(this.target, args);
				if (method.getName().equals("getExceptionListener") && retVal instanceof InternalChainedExceptionListener) {
					// Handle getExceptionListener method: hide internal chain.
					InternalChainedExceptionListener listener = (InternalChainedExceptionListener) retVal;
					return listener.getUserListener();
				}
				else {
					return retVal;
				}
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Internal chained ExceptionListener for handling the internal recovery listener
	 * in combination with a user-specified listener.
	 */
	private static class InternalChainedExceptionListener extends ChainedExceptionListener {

		public InternalChainedExceptionListener(ExceptionListener internalListener, ExceptionListener userListener) {
			addDelegate(internalListener);
			if (userListener != null) {
				addDelegate(userListener);
			}
		}

		public ExceptionListener getUserListener() {
			ExceptionListener[] delegates = getDelegates();
			return (delegates.length > 1 ? delegates[1] : null);
		}
	}

}
