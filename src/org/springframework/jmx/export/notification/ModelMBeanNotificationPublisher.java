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

package org.springframework.jmx.export.notification;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.springframework.util.Assert;

/**
 * {@link NotificationPublisher} implementation that uses the infrastructure
 * provided by the {@link javax.management.modelmbean.ModelMBean} interface to track
 * {@link javax.management.NotificationListener javax.management.NotificationListeners}
 * and send {@link javax.management.Notification Notifications} to those listeners.
 *
 * @author Rob Harrop
 * @author Rick Evans
 * @since 2.0
 * @see javax.management.modelmbean.ModelMBean
 * @see NotificationPublisherAware
 */
public class ModelMBeanNotificationPublisher implements NotificationPublisher {

	/**
	 * The {@link javax.management.modelmbean.ModelMBean} instance wrapping the managed resource into which this
	 * <code>NotificationPublisher</code> will be injected.
	 */
	private final ModelMBean modelMBean;

	/**
	 * The {@link javax.management.ObjectName} associated with the {@link javax.management.modelmbean.ModelMBean modelMBean}.
	 */
	private final ObjectName objectName;

	/**
	 * The managed resource associated with the {@link javax.management.modelmbean.ModelMBean modelMBean}.
	 */
	private final Object managedResource;


	/**
	 * Creates a new instance of the {@link org.springframework.jmx.export.notification.ModelMBeanNotificationPublisher} class
	 * that will publish all {@link javax.management.Notification Notifications}
	 * to the supplied {@link javax.management.modelmbean.ModelMBean}.
	 * @param modelMBean the target {@link javax.management.modelmbean.ModelMBean}; must not be <code>null</code>
	 * @param objectName the {@link javax.management.ObjectName} of the source {@link javax.management.modelmbean.ModelMBean}
	 * @param managedResource the managed resource exposed by the supplied {@link javax.management.modelmbean.ModelMBean}
	 * @throws IllegalArgumentException if any of the parameters is <code>null</code> 
	 */
	public ModelMBeanNotificationPublisher(ModelMBean modelMBean, ObjectName objectName, Object managedResource) {
		Assert.notNull(modelMBean, "The 'modelMBean' parameter must not be null.");
		Assert.notNull(objectName, "The 'objectName' parameter must not be null.");
		Assert.notNull(managedResource, "The 'managedResource' parameter must not be null.");
		this.modelMBean = modelMBean;
		this.objectName = objectName;
		this.managedResource = managedResource;
	}


	/**
	 * Sends the supplied {@link javax.management.Notification} using the wrapped
	 * {@link javax.management.modelmbean.ModelMBean} instance.
	 * @param notification the {@link javax.management.Notification} to be sent
	 * @throws IllegalArgumentException if the supplied <code>notification</code> is <code>null</code>
	 * @throws UnableToSendNotificationException if the supplied <code>notification</code> could not be sent
	 */
	public void sendNotification(Notification notification) {
		Assert.notNull(notification, "Notification must not be null");
		replaceNotificationSourceIfNecessary(notification);
		try {
			if (notification instanceof AttributeChangeNotification) {
				this.modelMBean.sendAttributeChangeNotification((AttributeChangeNotification) notification);
			}
			else {
				this.modelMBean.sendNotification(notification);
			}
		}
		catch (MBeanException ex) {
			throw new UnableToSendNotificationException("Unable to send notification [" + notification + "]", ex);
		}
	}

	/**
	 * From the {@link javax.management.Notification class level Javadoc}:
	 * <p><i>'It is strongly recommended that notification senders use the object name
	 * rather than a reference to the MBean object as the source.'</i>
	 *
	 * @param notification the {@link javax.management.Notification} whose {@link javax.management.Notification#getSource()} might need massaging
	 */
	private void replaceNotificationSourceIfNecessary(Notification notification) {
		if (notification.getSource() == null
				|| notification.getSource().equals(this.managedResource)) {
			notification.setSource(this.objectName);
		}
	}

}
