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

package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * Strategy interface for resolving JMS destinations.
 * 
 * <p>Used by {@link org.springframework.jms.core.JmsTemplate} for resolving
 * destination names from simple {@link String Strings} to actual
 * {@link javax.jms.Destination} implementation instances.
 *
 * <p>The default {@link org.springframework.jms.support.destination.DestinationResolver} implementation used by
 * {@link org.springframework.jms.core.JmsTemplate} instances is the
 * {@link DynamicDestinationResolver} class. Consider using the
 * {@link JndiDestinationResolver} for more advanced scenarios.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#setDestinationResolver
 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
 * @see org.springframework.jms.support.destination.JndiDestinationResolver
 */
public interface DestinationResolver {

	/**
	 * Resolve the given destination name, either as located resource
	 * or as dynamic destination.
	 * @param session the current JMS Session
	 * @param destinationName the name of the destination
	 * @param pubSubDomain <code>true</code> if the domain is pub-sub, <code>false</code> if P2P
	 * @return the JMS destination (either a topic or a queue)
	 * @throws javax.jms.JMSException if resolution failed
	 */
	Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException;

}
