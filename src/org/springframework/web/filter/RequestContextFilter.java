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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet filter that exposes the request to the current thread,
 * through both LocaleContextHolder and RequestContextHolder.
 *
 * <p>Useful for application objects that need access to the current request.
 * Does not require any configuration parameters.
 *
 * <p>Alternatively, Spring's RequestContextListener and Spring's DispatcherServlet
 * also expose the same request context to the current thread.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.context.request.RequestContextHolder
 * @see org.springframework.web.context.request.RequestContextListener
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextFilter extends OncePerRequestFilter {

	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		LocaleContextHolder.setLocale(request.getLocale());
		ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(requestAttributes);
		if (logger.isDebugEnabled()) {
			logger.debug("Bound request context to thread: " + request);
		}
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			requestAttributes.requestCompleted();
			RequestContextHolder.setRequestAttributes(null);
			LocaleContextHolder.setLocale(null);
			if (logger.isDebugEnabled()) {
				logger.debug("Cleared thread-bound request context: " + request);
			}
		}
	}

}
