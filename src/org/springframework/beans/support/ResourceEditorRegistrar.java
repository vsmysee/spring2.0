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

package org.springframework.beans.support;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.ClassEditor;
import org.springframework.beans.propertyeditors.FileEditor;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.beans.propertyeditors.URLEditor;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * PropertyEditorRegistrar implementation that populates a given PropertyEditorRegistry
 * (typically a BeanWrapper used for bean creation within an ApplicationContext)
 * with resource editors. Used by AbstractApplicationContext.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

	private final ResourceLoader resourceLoader;


	/**
	 * Create a new ResourceEditorRegistrar for the given ResourceLoader
	 * @param resourceLoader the ResourceLoader (or ResourcePatternResolver)
	 * to create editors for (usually an ApplicationContext)
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.context.ApplicationContext
	 */
	public ResourceEditorRegistrar(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	/**
	 * Populate the given bean factory with the following resource editors:
	 * ResourceEditor, URLEditor, FileEditor, InputStreamEditor.
	 * In case of a DefaultResourceLoader, a ClassEditor will be registered as well;
	 * in case of a ResourcePatternResolver, a ResourceArrayPropertyEditor will be registered.
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.beans.propertyeditors.URLEditor
	 * @see org.springframework.beans.propertyeditors.InputStreamEditor
	 * @see org.springframework.context.ApplicationContext
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader);
		registry.registerCustomEditor(Resource.class, baseEditor);
		registry.registerCustomEditor(URL.class, new URLEditor(baseEditor));
		registry.registerCustomEditor(File.class, new FileEditor(baseEditor));
		registry.registerCustomEditor(InputStream.class, new InputStreamEditor(baseEditor));

		if (this.resourceLoader instanceof DefaultResourceLoader) {
			ClassLoader classLoader = ((DefaultResourceLoader) this.resourceLoader).getClassLoader();
			registry.registerCustomEditor(Class.class, new ClassEditor(classLoader));
		}

		if (this.resourceLoader instanceof ResourcePatternResolver) {
			registry.registerCustomEditor(Resource[].class,
					new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader));
		}
	}

}
