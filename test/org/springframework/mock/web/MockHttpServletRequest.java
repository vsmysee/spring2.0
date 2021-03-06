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

package org.springframework.mock.web;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.CollectionFactory;
import org.springframework.util.Assert;

/**
 * Stub implementation of the {@link javax.servlet.http.HttpServletRequest} interface.
 *
 * <p>Used for testing the web framework; also useful for testing
 * application controllers.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Tamas Szabo
 * @since 1.0.2
 */
public class MockHttpServletRequest implements HttpServletRequest {

	/**
	 * The default protocol: 'http'.
	 */
	public static final String DEFAULT_PROTOCOL = "http";

	/**
	 * The default server address: '127.0.0.1'.
	 */
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1";

	/**
	 * The default server name: 'localhost'.
	 */
	public static final String DEFAULT_SERVER_NAME = "localhost";

	/**
	 * The default server port: '80'.
	 */
	public static final int DEFAULT_SERVER_PORT = 80;

	/**
	 * The default remote address: '127.0.0.1'.
	 */
	public static final String DEFAULT_REMOTE_ADDR = "127.0.0.1";

	/**
	 * The default remote host: 'localhost'.
	 */
	public static final String DEFAULT_REMOTE_HOST = "localhost";


	//---------------------------------------------------------------------
	// ServletRequest properties
	//---------------------------------------------------------------------

	private final Hashtable attributes = new Hashtable();

	private String characterEncoding;

	private byte[] content;

	private String contentType;

	private final Map parameters = CollectionFactory.createLinkedMapIfPossible(16);

	private String protocol = DEFAULT_PROTOCOL;

	private String scheme = DEFAULT_PROTOCOL;

	private String serverName = DEFAULT_SERVER_NAME;

	private int serverPort = DEFAULT_SERVER_PORT;

	private String remoteAddr = DEFAULT_REMOTE_ADDR;

	private String remoteHost = DEFAULT_REMOTE_HOST;

	/** List of locales in descending order */
	private final Vector locales = new Vector();

	private boolean secure = false;

	private final ServletContext servletContext;

	private int remotePort = DEFAULT_SERVER_PORT;

	private String localName = DEFAULT_SERVER_NAME;

	private String localAddr = DEFAULT_SERVER_ADDR;

	private int localPort = DEFAULT_SERVER_PORT;


	//---------------------------------------------------------------------
	// HttpServletRequest properties
	//---------------------------------------------------------------------

	private String authType;

	private Cookie[] cookies;

	/**
	 * The key is the lowercase header name; the value is a {@link org.springframework.mock.web.MockHttpServletRequest.HttpHeader} object.
	 */
	private final Map headers = new Hashtable();

	private String method;

	private String pathInfo;

	private String contextPath = "";

	private String queryString;

	private String remoteUser;

	private final Set userRoles = new HashSet();

	private Principal userPrincipal;

	private String requestURI = "";

	private String servletPath = "";

	private HttpSession session;

	private boolean requestedSessionIdValid = true;

	private boolean requestedSessionIdFromCookie = true;

	private boolean requestedSessionIdFromURL = false;


	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------

	/**
	 * Create a new MockHttpServletRequest.
	 * @param servletContext the ServletContext that the request runs in (can be <code>null</code>) 
	 */
	public MockHttpServletRequest(ServletContext servletContext) {
		this.locales.add(Locale.ENGLISH);
		this.servletContext = servletContext;
	}

	/**
	 * Create a new MockHttpServletRequest.
	 * @param servletContext the ServletContext that the request runs in (can be <code>null</code>)
	 * @param method the request method
	 * @param requestURI the request URI
	 * @see #setMethod
	 * @see #setRequestURI
	 */
	public MockHttpServletRequest(ServletContext servletContext, String method, String requestURI) {
		this(servletContext);
		this.method = method;
		this.requestURI = requestURI;
	}

	/**
	 * Create a new MockHttpServletRequest with a {@link MockServletContext}.
	 * @see MockServletContext
	 */
	public MockHttpServletRequest() {
		this(new MockServletContext());
	}

	/**
	 * Create a new MockHttpServletRequest with a MockServletContext.
	 * @param method the request method
	 * @param requestURI the request URI
	 * @see #setMethod
	 * @see #setRequestURI
	 * @see MockServletContext
	 */
	public MockHttpServletRequest(String method, String requestURI) {
		this(new MockServletContext(), method, requestURI);
	}


	//---------------------------------------------------------------------
	// ServletRequest interface
	//---------------------------------------------------------------------

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		return this.attributes.keys();
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public int getContentLength() {
		return (this.content != null ? this.content.length : -1);
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentType() {
		return contentType;
	}

	public ServletInputStream getInputStream() {
		if (this.content != null) {
			return new DelegatingServletInputStream(new ByteArrayInputStream(this.content));
		}
		else {
			return null;
		}
	}

	/**
	 * Set a single value for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String value) {
		setParameter(name, new String[] {value});
	}

	/**
	 * Set an array of values for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, they will be replaced.
	 */
	public void setParameter(String name, String[] values) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.put(name, values);
	}

	/**
	 * Add a single value for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, the given value will be added to the end of the list.
	 */
	public void addParameter(String name, String value) {
		addParameter(name, new String[] {value});
	}

	/**
	 * Add an array of values for the specified HTTP parameter.
	 * <p>If there are already one or more values registered for the given
	 * parameter name, the given values will be added to the end of the list.
	 */
	public void addParameter(String name, String[] values) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] oldArr = (String[]) this.parameters.get(name);
		if (oldArr != null) {
			String[] newArr = new String[oldArr.length + values.length];
			System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
			System.arraycopy(values, 0, newArr, oldArr.length, values.length);
			this.parameters.put(name, newArr);
		}
		else {
			this.parameters.put(name, values);
		}
	}

	/**
	 * Remove already registered values for the specified HTTP parameter, if any.
	 */
	public void removeParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		this.parameters.remove(name);
	}

	public String getParameter(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		String[] arr = (String[]) this.parameters.get(name);
		return (arr != null && arr.length > 0 ? arr[0] : null);
	}

	public Enumeration getParameterNames() {
		return Collections.enumeration(this.parameters.keySet());
	}

	public String[] getParameterValues(String name) {
		Assert.notNull(name, "Parameter name must not be null");
		return (String[]) this.parameters.get(name);
	}

	public Map getParameterMap() {
		return Collections.unmodifiableMap(this.parameters);
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public String getScheme() {
		return scheme;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public int getServerPort() {
		return serverPort;
	}

	public BufferedReader getReader() throws UnsupportedEncodingException {
		if (this.content != null) {
			InputStream sourceStream = new ByteArrayInputStream(this.content);
			Reader sourceReader = (this.characterEncoding != null) ?
				new InputStreamReader(sourceStream, this.characterEncoding) : new InputStreamReader(sourceStream);
			return new BufferedReader(sourceReader);
		}
		else {
			return null;
		}
	}

	public void setRemoteAddr(String remoteAddr) {
		this.remoteAddr = remoteAddr;
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setAttribute(String name, Object value) {
		Assert.notNull(name, "Attribute name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			this.attributes.remove(name);
		}
	}

	public void removeAttribute(String name) {
		Assert.notNull(name, "Attribute name must not be null");
		this.attributes.remove(name);
	}

	/**
	 * Add a new preferred locale, before any existing locales.
	 */
	public void addPreferredLocale(Locale locale) {
		Assert.notNull(locale, "Locale must not be null");
		this.locales.add(0, locale);
	}

	public Locale getLocale() {
		return (Locale) this.locales.get(0);
	}

	public Enumeration getLocales() {
		return this.locales.elements();
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isSecure() {
		return secure;
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return new MockRequestDispatcher(path);
	}

	public String getRealPath(String path) {
		return this.servletContext.getRealPath(path);
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setLocalName(String localName) {
		this.localName = localName;
	}

	public String getLocalName() {
		return localName;
	}

	public void setLocalAddr(String localAddr) {
		this.localAddr = localAddr;
	}

	public String getLocalAddr() {
		return localAddr;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public int getLocalPort() {
		return localPort;
	}


	//---------------------------------------------------------------------
	// HttpServletRequest interface
	//---------------------------------------------------------------------

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	public String getAuthType() {
		return authType;
	}

	public void setCookies(Cookie[] cookies) {
		this.cookies = cookies;
	}

	public Cookie[] getCookies() {
		return cookies;
	}

	/**
	 * Add a header entry for the given name.
	 * <p>If there was no entry for that header name before,
	 * the value will be used as-is. In case of an existing entry,
	 * a String array will be created, adding the given value (more
	 * specifically, its toString representation) as further element.
	 * <p>Multiple values can only be stored as list of Strings,
	 * following the Servlet spec (see <code>getHeaders</code> accessor).
	 * As alternative to repeated <code>addHeader</code> calls for
	 * individual elements, you can use a single call with an entire
	 * array or Collection of values as parameter.
	 * @see #getHeaderNames
	 * @see #getHeader
	 * @see #getHeaders
	 * @see #getDateHeader
	 * @see #getIntHeader
	 */
	public void addHeader(String name, Object value) {
		Assert.notNull(name, "Header name must not be null");
		Assert.notNull(value, "Header value must not be null");
		HttpHeader header = (HttpHeader) this.headers.get(name.toLowerCase());
		if (header == null) {
			header = new HttpHeader(name);
			this.headers.put(name.toLowerCase(), header);
		}
		if (value instanceof Collection) {
			header.addValues((Collection) value);
		}
		else if (value.getClass().isArray()) {
			header.addValues((Object[]) value);
		}
		else {
			header.addValue(value);
		}
	}

	public long getDateHeader(String name) {
		Assert.notNull(name, "Header name must not be null");
		HttpHeader header = (HttpHeader) this.headers.get(name.toLowerCase());
		Object value = (header == null) ? null : header.getValue();
		if (value instanceof Date) {
			return ((Date) value).getTime();
		}
		else if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		else if (value != null) {
			throw new IllegalArgumentException(
					"Value for header '" + name + "' is neither a Date nor a Number: " + value);
		}
		else {
			return -1L;
		}
	}

	public String getHeader(String name) {
		HttpHeader header = (HttpHeader) this.headers.get(name.toLowerCase());
		return header == null ? null : header.getValue().toString();
	}

	public Enumeration getHeaders(String name) {
		HttpHeader header = (HttpHeader) this.headers.get(name.toLowerCase());
		return Collections.enumeration(
				(header == null) ? Collections.EMPTY_LIST : header.getValues());
	}

	public Enumeration getHeaderNames() {
		return new HttpHeaderNamesEnumerator(this.headers.values());
	}

	public int getIntHeader(String name) {
		Assert.notNull(name, "Header name must not be null");
		HttpHeader header = (HttpHeader)this.headers.get(name.toLowerCase());
        Object value = (header == null) ? null : header.getValue();
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		else if (value instanceof String) {
			return Integer.parseInt((String) value);
		}
		else if (value != null) {
			throw new NumberFormatException("Value for header '" + name + "' is not a Number : " + value);
		}
		else {
			return -1;
		}
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	public void setPathInfo(String pathInfo) {
		this.pathInfo = pathInfo;
	}

	public String getPathInfo() {
		return pathInfo;
	}

	public String getPathTranslated() {
		return (this.pathInfo != null ? getRealPath(this.pathInfo) : null);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getQueryString() {
		return queryString;
	}

	public void setRemoteUser(String remoteUser) {
		this.remoteUser = remoteUser;
	}

	public String getRemoteUser() {
		return remoteUser;
	}

	/**
	 * @deprecated in favor of addUserRole
	 * @see #addUserRole
	 */
	public void addRole(String role) {
		addUserRole(role);
	}

	public void addUserRole(String role) {
		this.userRoles.add(role);
	}

	public boolean isUserInRole(String role) {
		return this.userRoles.contains(role);
	}

	public void setUserPrincipal(Principal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	public String getRequestedSessionId() {
		HttpSession session = this.getSession();
		return (session != null ? session.getId() : null);
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public StringBuffer getRequestURL() {
		StringBuffer url = new StringBuffer(this.scheme);
		url.append("://").append(this.serverName).append(':').append(this.serverPort);
		url.append(getRequestURI());
		return url;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	public String getServletPath() {
		return servletPath;
	}

	public void setSession(HttpSession session) {
		this.session = session;
		if (session instanceof MockHttpSession) {
			MockHttpSession mockSession = ((MockHttpSession) session);
			mockSession.access();
		}
	}

	public HttpSession getSession(boolean create) {
		// reset session if invalidated
		if (this.session instanceof MockHttpSession && ((MockHttpSession) this.session).isInvalid()) {
			this.session = null;
		}
		// create new session if necessary
		if (this.session == null && create) {
			this.session = new MockHttpSession(this.servletContext);
		}
		return this.session;
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public void setRequestedSessionIdValid(boolean requestedSessionIdValid) {
		this.requestedSessionIdValid = requestedSessionIdValid;
	}

	public boolean isRequestedSessionIdValid() {
		return this.requestedSessionIdValid;
	}

	public void setRequestedSessionIdFromCookie(boolean requestedSessionIdFromCookie) {
		this.requestedSessionIdFromCookie = requestedSessionIdFromCookie;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return this.requestedSessionIdFromCookie;
	}

	public void setRequestedSessionIdFromURL(boolean requestedSessionIdFromURL) {
		this.requestedSessionIdFromURL = requestedSessionIdFromURL;
	}

	public boolean isRequestedSessionIdFromURL() {
		return this.requestedSessionIdFromURL;
	}

	public boolean isRequestedSessionIdFromUrl() {
		return isRequestedSessionIdFromURL();
	}


	private static final class HttpHeader {

		private String name;
		private List values = new LinkedList();


		HttpHeader(String name) {
			Assert.notNull(name, "The header name cannot be null.");
			this.name = name;
		}


		void addValue(Object value) {
			Assert.notNull(values, "Value must not be null.");
			this.values.add(value);
		}

		void addValues(Collection values) {
			Assert.notNull(values, "Values collection must not be null.");
			for (Iterator it = values.iterator(); it.hasNext();) {
				Object element = it.next();
				Assert.notNull(element, "Value collection must not contain null elements.");
				this.values.add(element);
			}
		}

		void addValues(Object[] values) {
			this.values.addAll(Arrays.asList(values));
		}

		String getName() {
			return this.name;
		}

		List getValues() {
			return this.values;
		}

		Object getValue() {
			return this.values.isEmpty() ? null : this.values.get(0);
		}

	}


	private static final class HttpHeaderNamesEnumerator implements Enumeration {

		private Iterator httpHeaderIterator;


		HttpHeaderNamesEnumerator(Collection headers) {
			Assert.notNull("Headers must not be null.");
			this.httpHeaderIterator = headers.iterator();
		}


		public boolean hasMoreElements() {
			return this.httpHeaderIterator.hasNext();
		}

		public Object nextElement() {
			return ((HttpHeader) this.httpHeaderIterator.next()).getName();
		}

	}

}
