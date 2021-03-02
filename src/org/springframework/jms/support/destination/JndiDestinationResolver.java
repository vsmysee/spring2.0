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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.NamingException;

import org.springframework.jndi.JndiLocatorSupport;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.jms.support.destination.DestinationResolver} implementation which interprets destination names
 * as JNDI locations (with a configurable fallback strategy).
 *
 * <p>Allows for customizing the JNDI environment if necessary,
 * for example specifying appropriate JNDI environment properties.
 *
 * <p>Dynamic queues and topics get cached by destination name.
 * Thus, use unique destination names across both queues and topics.
 * Caching can be turned off by specifying {@link #cache} as <code>false</code>.
 *
 * <b>Please note that the fallback to resolution of dynamic destinations is turned
 * off by default. Specify
 * {@link #fallbackToDynamicDestination fallbackToDynamicDestination} as
 * <code>true</code> to enable this functionality.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setCache
 * @see #setFallbackToDynamicDestination
 */
public class JndiDestinationResolver extends JndiLocatorSupport implements CachingDestinationResolver {

	private boolean cache = true;

	private boolean fallbackToDynamicDestination = false;

	private DestinationResolver dynamicDestinationResolver = new DynamicDestinationResolver();

	private final Map destinationCache = Collections.synchronizedMap(new HashMap());


	/**
	 * Set whether to cache resolved destinations. Default is <code>true</code> .
	 * <p>Can be turned off to re-lookup a destination for each operation,
	 * which allows for hot restarting of destinations. This is mainly
	 * useful during development.
	 * <p>Please note that dynamic queues and topics get cached by destination name.
	 * Thus, use unique destination names across both queues and topics.
	 * @param cache <code>true</code> if resolved destinations are to be cached
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * Set the ability of {@link org.springframework.jms.support.destination.JndiDestinationResolver} to create dynamic destinations
	 * if the destination name is not found in JNDI. Default is <code>false</code>.
	 * @param fallbackToDynamicDestination <code>true</code> if this {@link org.springframework.jms.support.destination.JndiDestinationResolver} instance is to
	 * fallback to resolving destinations dynamically
	 * @see #setDynamicDestinationResolver(org.springframework.jms.support.destination.DestinationResolver)
	 */
	public void setFallbackToDynamicDestination(boolean fallbackToDynamicDestination) {
		this.fallbackToDynamicDestination = fallbackToDynamicDestination;
	}

	/**
	 * Set the {@link org.springframework.jms.support.destination.DestinationResolver} to use when falling back to dynamic
	 * destinations.
	 * <p>The default is a {@link org.springframework.jms.support.destination.DynamicDestinationResolver}.
	 * @param dynamicDestinationResolver the {@link org.springframework.jms.support.destination.DestinationResolver} to use
	 * when falling back to dynamic destinations
	 * @see #setFallbackToDynamicDestination
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 */
	public void setDynamicDestinationResolver(DestinationResolver dynamicDestinationResolver) {
		this.dynamicDestinationResolver = dynamicDestinationResolver;
	}


	public Destination resolveDestinationName(Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.notNull(destinationName, "Destination name must not be null");
		Destination dest = (Destination) this.destinationCache.get(destinationName);
		if (dest != null) {
			validateDestination(dest, destinationName, pubSubDomain);
		}
		else {
			try {
				dest = (Destination) lookup(destinationName, Destination.class);
				validateDestination(dest, destinationName, pubSubDomain);
			}
			catch (NamingException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Destination [" + destinationName + "] not found in JNDI", ex);
				}
				if (this.fallbackToDynamicDestination) {
					dest = this.dynamicDestinationResolver.resolveDestinationName(session, destinationName, pubSubDomain);
				}
				else {
					throw new DestinationResolutionException(
							"Destination [" + destinationName + "] not found in JNDI", ex);
				}
			}
			if (this.cache) {
				this.destinationCache.put(destinationName, dest);
			}
		}
		return dest;
	}

	/**
	 * Validate the given Destination object, checking whether it matches
	 * the expected type.
	 * @param destination the Destination object to validate
	 * @param destinationName the name of the destination
	 * @param pubSubDomain <code>true</code> if a Topic is expected,
	 * <code>false</code> in case of a Queue
	 */
	protected void validateDestination(Destination destination, String destinationName, boolean pubSubDomain) {
		Class targetClass = Queue.class;
		if (pubSubDomain) {
			targetClass = Topic.class;
		}
		if (!targetClass.isInstance(destination)) {
			throw new DestinationResolutionException(
					"Destination [" + destinationName + "] is not of expected type [" + targetClass.getName() + "]");
		}
	}


	public void removeFromCache(String destinationName) {
		this.destinationCache.remove(destinationName);
	}

	public void clearCache() {
		this.destinationCache.clear();
	}

}
