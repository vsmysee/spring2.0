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

package org.springframework.core.io.support;

import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

/**
 * Utility class for determining whether a given URL is a resource
 * location that can be loaded via a ResourcePatternResolver.
 *
 * <p>Callers will usually assume that a location is a relative path
 * if the <code>isUrl</code> method returns <code>false</code>.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see #isUrl(String)
 * @see org.springframework.util.ResourceUtils#isUrl(String)
 */
public abstract class ResourcePatternUtils {

	/**
	 * Return whether the given resource location is a URL: either a
	 * special "classpath" or "classpath*" pseudo URL or a standard URL.
	 * @see org.springframework.core.io.support.ResourcePatternResolver#CLASSPATH_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX
	 * @see org.springframework.util.ResourceUtils#isUrl(String)
	 * @see java.net.URL
	 */
	public static boolean isUrl(String resourceLocation) {
		if (resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
			return true;
		}
		return ResourceUtils.isUrl(resourceLocation);
	}

	/**
	 * Return a default ResourcePatternResolver for the given ResourceLoader.
	 * <p>This might be the ResourceLoader itself, if it implements the
	 * ResourcePatternResolver extension, or a PathMatchingResourcePatternResolver
	 * built on the given ResourceLoader.
	 * @param resourceLoader the ResourceLoader to build a pattern resolver for
	 * (not <code>null</code>)
	 * @return the ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	public static ResourcePatternResolver getResourcePatternResolver(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		if (resourceLoader instanceof ResourcePatternResolver) {
			return (ResourcePatternResolver) resourceLoader;
		}
		else {
			return new PathMatchingResourcePatternResolver(resourceLoader);
		}
	}

}