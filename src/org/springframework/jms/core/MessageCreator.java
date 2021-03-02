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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Creates a JMS message given a {@link javax.jms.Session}.
 * 
 * <p>The <code>Session</code> typically is provided by an instance
 * of the {@link org.springframework.jms.core.JmsTemplate} class.
 *
 * <p>Implementations <i>do not</i> need to concern themselves with
 * checked <code>JMSExceptions</code> (from the '<code>javax.jms</code>'
 * package) that may be thrown from operations they attempt. The
 * <code>JmsTemplate</code> will catch and handle these
 * <code>JMSExceptions</code> appropriately.
 *
 * @author Mark Pollack
 * @since 1.1
 */
public interface MessageCreator {

	/**
	 * Create a {@link javax.jms.Message} to be sent.
	 * @param session the JMS {@link javax.jms.Session} to be used to create the
	 * <code>Message</code> (never <code>null</code>) 
	 * @return the <code>Message</code> to be sent
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	Message createMessage(Session session) throws JMSException;

}
