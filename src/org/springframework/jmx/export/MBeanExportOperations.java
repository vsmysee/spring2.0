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

package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * Interface that defines the set of MBean export operations that are intended to be
 * accessed by application developers during application runtime.
 *
 * <p>This interface should be used to export application resources to JMX using Spring's
 * management interface generation capabilties and, optionally, it's {@link javax.management.ObjectName}
 * generation capabilities.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface MBeanExportOperations {

	/**
	 * Register the supplied resource with JMX. If the resource is not a valid MBean already,
	 * Spring will generate a management interface for it. The exact interface generated will
	 * depend on the implementation and its configuration. This call also generates an
	 * {@link javax.management.ObjectName} for the managed resource and returns this to the caller.
	 * @param managedResource the resource to expose via JMX
	 * @return the {@link javax.management.ObjectName} under which the resource was exposed
	 * @throws org.springframework.jmx.export.MBeanExportException if Spring is unable to generate an {@link javax.management.ObjectName}
	 * or register the MBean
	 */
	ObjectName registerManagedResource(Object managedResource) throws MBeanExportException;

	/**
	 * Register the supplied resource with JMX. If the resource is not a valid MBean already,
	 * Spring will generate a management interface for it. The exact interface generated will
	 * depend on the implementation and its configuration.
	 * @param managedResource the resource to expose via JMX
	 * @param objectName the {@link javax.management.ObjectName} under which to expose the resource
	 * @throws org.springframework.jmx.export.MBeanExportException if Spring is unable to register the MBean
	 */
	void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException;

}
