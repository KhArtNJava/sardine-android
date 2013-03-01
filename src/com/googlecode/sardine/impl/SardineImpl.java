/*
 * Copyright 2009-2011 Jon Stevens et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.sardine.impl;

import ch.boye.httpclientandroidlib.Header;
import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpHeaders;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.HttpStatus;
import ch.boye.httpclientandroidlib.HttpVersion;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.NTCredentials;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.client.AuthCache;
import ch.boye.httpclientandroidlib.client.HttpResponseException;
import ch.boye.httpclientandroidlib.client.ResponseHandler;
import ch.boye.httpclientandroidlib.client.methods.HttpDelete;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.methods.HttpHead;
import ch.boye.httpclientandroidlib.client.methods.HttpPut;
import ch.boye.httpclientandroidlib.client.methods.HttpRequestBase;
import ch.boye.httpclientandroidlib.client.methods.HttpUriRequest;
import ch.boye.httpclientandroidlib.client.params.AuthPolicy;
import ch.boye.httpclientandroidlib.client.protocol.ClientContext;
import ch.boye.httpclientandroidlib.client.protocol.RequestAcceptEncoding;
import ch.boye.httpclientandroidlib.client.protocol.ResponseContentEncoding;
import ch.boye.httpclientandroidlib.conn.ClientConnectionManager;
import ch.boye.httpclientandroidlib.conn.routing.HttpRoutePlanner;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.entity.ByteArrayEntity;
import ch.boye.httpclientandroidlib.entity.InputStreamEntity;
import ch.boye.httpclientandroidlib.entity.StringEntity;
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.client.AbstractHttpClient;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.client.DefaultRedirectStrategy;
import ch.boye.httpclientandroidlib.impl.conn.PoolingClientConnectionManager;
import ch.boye.httpclientandroidlib.impl.conn.ProxySelectorRoutePlanner;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.params.HttpProtocolParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;
import ch.boye.httpclientandroidlib.protocol.HTTP;
import ch.boye.httpclientandroidlib.protocol.HttpContext;
import ch.boye.httpclientandroidlib.util.VersionInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.googlecode.sardine.DavAce;
import com.googlecode.sardine.DavAcl;
import com.googlecode.sardine.DavPrincipal;
import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.Version;
import com.googlecode.sardine.impl.handler.ExistsResponseHandler;
import com.googlecode.sardine.impl.handler.LockResponseHandler;
import com.googlecode.sardine.impl.handler.MultiStatusResponseHandler;
import com.googlecode.sardine.impl.handler.VoidResponseHandler;
import com.googlecode.sardine.impl.io.ConsumingInputStream;
import com.googlecode.sardine.impl.methods.HttpAcl;
import com.googlecode.sardine.impl.methods.HttpCopy;
import com.googlecode.sardine.impl.methods.HttpLock;
import com.googlecode.sardine.impl.methods.HttpMkCol;
import com.googlecode.sardine.impl.methods.HttpMove;
import com.googlecode.sardine.impl.methods.HttpPropFind;
import com.googlecode.sardine.impl.methods.HttpPropPatch;
import com.googlecode.sardine.impl.methods.HttpUnlock;
import com.googlecode.sardine.model.Ace;
import com.googlecode.sardine.model.Acl;
import com.googlecode.sardine.model.Allprop;
import com.googlecode.sardine.model.Displayname;
import com.googlecode.sardine.model.Exclusive;
import com.googlecode.sardine.model.Group;
import com.googlecode.sardine.model.Lockinfo;
import com.googlecode.sardine.model.Lockscope;
import com.googlecode.sardine.model.Locktype;
import com.googlecode.sardine.model.Multistatus;
import com.googlecode.sardine.model.Owner;
import com.googlecode.sardine.model.PrincipalCollectionSet;
import com.googlecode.sardine.model.PrincipalURL;
import com.googlecode.sardine.model.Prop;
import com.googlecode.sardine.model.Propertyupdate;
import com.googlecode.sardine.model.Propfind;
import com.googlecode.sardine.model.Propstat;
import com.googlecode.sardine.model.Remove;
import com.googlecode.sardine.model.Resourcetype;
import com.googlecode.sardine.model.Response;
import com.googlecode.sardine.model.Set;
import com.googlecode.sardine.model.Write;
import com.googlecode.sardine.util.SardineUtil;
import java.net.ProtocolException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import name.khartn.MySSLSocketFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

/**
 * Implementation of the Sardine interface. This is where the meat of the Sardine library lives.
 *
 * @author jonstevens
 * @version $Id: SardineImpl.java 350 2012-07-30 19:53:12Z latchkey $
 */
public class SardineImpl implements Sardine
{
	private static Logger log = Logger.getLogger(DavResource.class);

	private static final String UTF_8 = "UTF-8";

	/**
	 * HTTP Implementation
	 */
	private AbstractHttpClient client;

	/**
	 * Local context with authentication cache. Make sure the same context is used to execute
	 * logically related requests.
	 */
	private HttpContext context = new BasicHttpContext();

	/**
	 * Access resources with no authentication
	 */
	public SardineImpl()
	{
		this(null, null);
	}

	/**
	 * Supports standard authentication mechanisms
	 *
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	public SardineImpl(String username, String password)
	{
		this(username, password, null);
	}

	/**
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 * @param selector Proxy configuration
	 */
	public SardineImpl(String username, String password, ProxySelector selector)
	{
		this.init(this.createDefaultClient(selector), username, password);
	}

	/**
	 * @param http Custom client configuration
	 */
	public SardineImpl(AbstractHttpClient http)
	{
		this(http, null, null);
	}

	/**
	 * @param http	 Custom client configuration
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	public SardineImpl(AbstractHttpClient http, String username, String password)
	{
		this.init(http, username, password);
	}

	private void init(AbstractHttpClient http, String username, String password)
	{
		this.client = http;
		this.client.setRedirectStrategy(new DefaultRedirectStrategy()
		{
			@Override
			public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)  throws ch.boye.httpclientandroidlib.ProtocolException
			{
				int statusCode = response.getStatusLine().getStatusCode();
				String method = request.getRequestLine().getMethod();
				Header locationHeader = response.getFirstHeader("location");
				switch (statusCode)
				{
					case HttpStatus.SC_MOVED_TEMPORARILY:
						return (method.equalsIgnoreCase(HttpGet.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpHead.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpLock.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpAcl.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpPropFind.METHOD_NAME)) && (locationHeader != null);
					case HttpStatus.SC_MOVED_PERMANENTLY:
					case HttpStatus.SC_TEMPORARY_REDIRECT:
						return method.equalsIgnoreCase(HttpGet.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpHead.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpLock.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpAcl.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpPropFind.METHOD_NAME);
					case HttpStatus.SC_SEE_OTHER:
						return true;
					default:
						return false;
				}
			}

			@Override
			public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ch.boye.httpclientandroidlib.ProtocolException
					
			{
				String method = request.getRequestLine().getMethod();
				if (method.equalsIgnoreCase(HttpPropFind.METHOD_NAME))
				{
					return new HttpPropFind(this.getLocationURI(request, response, context));
				}
				if (method.equalsIgnoreCase(HttpLock.METHOD_NAME))
				{
					return new HttpLock(this.getLocationURI(request, response, context));
				}
				return super.getRedirect(request, response, context);
			}
		});
		this.setCredentials(username, password);
	}

	/**
	 * Add credentials to any scope. Supports Basic, Digest and NTLM authentication methods.
	 *
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	@Override
	public void setCredentials(String username, String password)
	{
		this.setCredentials(username, password, "", "");
	}

	/**
	 * @param username	Use in authentication header credentials
	 * @param password	Use in authentication header credentialsn
	 * @param domain	  NTLM authentication
	 * @param workstation NTLM authentication
	 */
	@Override
	public void setCredentials(String username, String password, String domain, String workstation)
	{
		if (username != null)
		{
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.NTLM),
					new NTCredentials(username, password, workstation, domain));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.BASIC),
					new UsernamePasswordCredentials(username, password));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.DIGEST),
					new UsernamePasswordCredentials(username, password));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.SPNEGO),
					new UsernamePasswordCredentials(username, password));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.KERBEROS),
					new UsernamePasswordCredentials(username, password));
		}
	}

	/**
	 * Adds handling of GZIP compression to the client.
	 */
	@Override
	public void enableCompression()
	{
		this.client.addRequestInterceptor(new RequestAcceptEncoding());
		this.client.addResponseInterceptor(new ResponseContentEncoding());
	}

	/**
	 * Disable GZIP compression header.
	 */
	@Override
	public void disableCompression()
	{
		this.client.removeRequestInterceptorByClass(RequestAcceptEncoding.class);
		this.client.removeResponseInterceptorByClass(ResponseContentEncoding.class);
	}

	@Override
	public void enablePreemptiveAuthentication(String hostname)
	{
		AuthCache authCache = new BasicAuthCache();
		// Generate Basic preemptive scheme object and stick it to the local execution context
		BasicScheme basicAuth = new BasicScheme();
		SchemeRegistry registry = this.client.getConnectionManager().getSchemeRegistry();
		// Configure HttpClient to authenticate preemptively by prepopulating the authentication data cache.
		for (String scheme : registry.getSchemeNames())
		{
			int port = registry.getScheme(scheme).getDefaultPort();
			authCache.put(new HttpHost(hostname), basicAuth);
			authCache.put(new HttpHost(hostname, -1, scheme), basicAuth);
			authCache.put(new HttpHost(hostname, port, scheme), basicAuth);
		}
		// Add AuthCache to the execution context
		this.context.setAttribute(ClientContext.AUTH_CACHE, authCache);
	}

	@Override
	public void disablePreemptiveAuthentication()
	{
		this.context.removeAttribute(ClientContext.AUTH_CACHE);
	}

	@Override
	public List<DavResource> getResources(String url) throws IOException
	{
		return this.list(url);
	}

	@Override
	public List<DavResource> list(String url) throws IOException
	{
		return this.list(url, 1);
	}

	@Override
	public List<DavResource> list(String url, int depth) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth(Integer.toString(depth));
		Propfind body = new Propfind();
		body.setAllprop(new Allprop());
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response response : responses)
		{
			try
			{
				resources.add(new DavResource(response));
			}
			catch (URISyntaxException e)
			{
				log.warn(String.format("Ignore resource with invalid URI %s", response.getHref().get(0)));
			}
		}
		return resources;
	}

	@Override
	public void setCustomProps(String url, Map<String, String> set, List<String> remove) throws IOException
	{
		this.patch(url, SardineUtil.toQName(set), SardineUtil.toQName(remove));
	}

	@Override
	public List<DavResource> patch(String url, Map<QName, String> setProps) throws IOException
	{
		return this.patch(url, setProps, Collections.<QName>emptyList());
	}

	/**
	 * Creates a {@link com.googlecode.sardine.model.Propertyupdate} element containing all properties to set from setProps and all properties to
	 * remove from removeProps. Note this method will use a {@link com.googlecode.sardine.util.SardineUtil#CUSTOM_NAMESPACE_URI} as
	 * namespace and {@link com.googlecode.sardine.util.SardineUtil#CUSTOM_NAMESPACE_PREFIX} as prefix.
	 */
	@Override
	public List<DavResource> patch(String url, Map<QName, String> setProps, List<QName> removeProps) throws IOException
	{
		HttpPropPatch entity = new HttpPropPatch(url);
		// Build WebDAV <code>PROPPATCH</code> entity.
		Propertyupdate body = new Propertyupdate();
		// Add properties
		{
			Set set = new Set();
			body.getRemoveOrSet().add(set);
			Prop prop = new Prop();
			// Returns a reference to the live list
			List<Element> any = prop.getAny();
			for (Map.Entry<QName, String> entry : setProps.entrySet())
			{
				Element element = SardineUtil.createElement(entry.getKey());
				element.setTextContent(entry.getValue());
				any.add(element);
			}
			set.setProp(prop);
		}
		// Remove properties
		{
			Remove remove = new Remove();
			body.getRemoveOrSet().add(remove);
			Prop prop = new Prop();
			// Returns a reference to the live list
			List<Element> any = prop.getAny();
			for (QName entry : removeProps)
			{
				Element element = SardineUtil.createElement(entry);
				any.add(element);
			}
			remove.setProp(prop);
		}
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response response : responses)
		{
			try
			{
				resources.add(new DavResource(response));
			}
			catch (URISyntaxException e)
			{
				log.warn(String.format("Ignore resource with invalid URI %s", response.getHref().get(0)));
			}
		}
		return resources;
	}

	@Override
	public String lock(String url) throws IOException
	{
		HttpLock entity = new HttpLock(url);
		Lockinfo body = new Lockinfo();
		Lockscope scopeType = new Lockscope();
		scopeType.setExclusive(new Exclusive());
		body.setLockscope(scopeType);
		Locktype lockType = new Locktype();
		lockType.setWrite(new Write());
		body.setLocktype(lockType);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		// Return the lock token
		return this.execute(entity, new LockResponseHandler());
	}

	@Override
	public void unlock(String url, String token) throws IOException
	{
		HttpUnlock entity = new HttpUnlock(url, token);
		Lockinfo body = new Lockinfo();
		Lockscope scopeType = new Lockscope();
		scopeType.setExclusive(new Exclusive());
		body.setLockscope(scopeType);
		Locktype lockType = new Locktype();
		lockType.setWrite(new Write());
		body.setLocktype(lockType);
		this.execute(entity, new VoidResponseHandler());
	}

	@Override
	public void setAcl(String url, List<DavAce> aces) throws IOException
	{
		HttpAcl entity = new HttpAcl(url);
		// Build WebDAV <code>ACL</code> entity.
		Acl body = new Acl();
		body.setAce(new ArrayList<Ace>());
		for (DavAce davAce : aces)
		{
			// protected and inherited acl must not be part of ACL http request
			if (davAce.getInherited() != null || davAce.isProtected())
			{
				continue;
			}
			Ace ace = davAce.toModel();
			body.getAce().add(ace);
		}
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		this.execute(entity, new VoidResponseHandler());
	}


	@Override
	public DavAcl getAcl(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("0");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setOwner(new Owner());
		prop.setGroup(new Group());
		prop.setAcl(new Acl());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			return new DavAcl(responses.get(0));
		}
	}

	@Override
	public List<DavPrincipal> getPrincipals(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("1");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setDisplayname(new Displayname());
		prop.setResourcetype(new Resourcetype());
		prop.setPrincipalURL(new PrincipalURL());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			List<DavPrincipal> collections = new ArrayList<DavPrincipal>();
			for (Response r : responses)
			{
				if (r.getPropstat() != null)
				{
					for (Propstat propstat : r.getPropstat())
					{
						if (propstat.getProp() != null
								&& propstat.getProp().getResourcetype() != null
								&& propstat.getProp().getResourcetype().getPrincipal() != null)
						{
							collections.add(new DavPrincipal(DavPrincipal.PrincipalType.HREF,
									r.getHref().get(0),
									propstat.getProp().getDisplayname().getContent().get(0)));
						}
					}
				}
			}
			return collections;
		}
	}

	@Override
	public List<String> getPrincipalCollectionSet(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		entity.setDepth("0");
		Propfind body = new Propfind();
		Prop prop = new Prop();
		prop.setPrincipalCollectionSet(new PrincipalCollectionSet());
		body.setProp(prop);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = this.execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		if (responses.isEmpty())
		{
			return null;
		}
		else
		{
			List<String> collections = new ArrayList<String>();
			for (Response r : responses)
			{
				if (r.getPropstat() != null)
				{
					for (Propstat propstat : r.getPropstat())
					{
						if (propstat.getProp() != null
								&& propstat.getProp().getPrincipalCollectionSet() != null
								&& propstat.getProp().getPrincipalCollectionSet().getHref() != null)
						{
							collections.addAll(propstat.getProp().getPrincipalCollectionSet().getHref());
						}
					}
				}
			}
			return collections;
		}
	}

	@Override
	public InputStream get(String url) throws IOException
	{
		return this.get(url, Collections.<String, String>emptyMap());
	}

	@Override
	public InputStream get(String url, Map<String, String> headers) throws IOException
	{
		HttpGet get = new HttpGet(url);
		for (String header : headers.keySet())
		{
			get.addHeader(header, headers.get(header));
		}
		// Must use #execute without handler, otherwise the entity is consumed
		// already after the handler exits.
		HttpResponse response = this.execute(get);
		VoidResponseHandler handler = new VoidResponseHandler();
		try
		{
			handler.handleResponse(response);
			// Will consume the entity when the stream is closed.
			return new ConsumingInputStream(response);
		}
		catch (IOException ex)
		{
			get.abort();
			throw ex;
		}
	}

	@Override
	public void put(String url, byte[] data) throws IOException
	{
		this.put(url, data, null);
	}

	@Override
	public void put(String url, byte[] data, String contentType) throws IOException
	{
		ByteArrayEntity entity = new ByteArrayEntity(data);
		this.put(url, entity, contentType, true);
	}

	@Override
	public void put(String url, InputStream dataStream) throws IOException
	{
		this.put(url, dataStream, (String) null);
	}

	@Override
	public void put(String url, InputStream dataStream, String contentType) throws IOException
	{
		this.put(url, dataStream, contentType, true);
	}

	@Override
	public void put(String url, InputStream dataStream, String contentType, boolean expectContinue) throws IOException
	{
		// A length of -1 means "go until end of stream"
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		this.put(url, entity, contentType, expectContinue);
	}

	@Override
	public void put(String url, InputStream dataStream, Map<String, String> headers) throws IOException
	{
		// A length of -1 means "go until end of stream"
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		this.put(url, entity, headers);
	}

	/**
	 * Upload the entity using <code>PUT</code>
	 *
	 * @param url			Resource
	 * @param entity		 The entity to read from
	 * @param contentType	Content Type header
	 * @param expectContinue Add <code>Expect: continue</code> header
	 */
	public void put(String url, HttpEntity entity, String contentType, boolean expectContinue) throws IOException
	{
		Map<String, String> headers = new HashMap<String, String>();
		if (contentType != null)
		{
			headers.put(HttpHeaders.CONTENT_TYPE, contentType);
		}
		if (expectContinue)
		{
			headers.put(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);
		}
		this.put(url, entity, headers);
	}

	/**
	 * Upload the entity using <code>PUT</code>
	 *
	 * @param url	 Resource
	 * @param entity  The entity to read from
	 * @param headers Headers to add to request
	 */
	public void put(String url, HttpEntity entity, Map<String, String> headers) throws IOException
	{
		HttpPut put = new HttpPut(url);
		put.setEntity(entity);
		for (String header : headers.keySet())
		{
			put.addHeader(header, headers.get(header));
		}
		if (entity.getContentType() == null && !put.containsHeader(HttpHeaders.CONTENT_TYPE))
		{
			put.addHeader(HttpHeaders.CONTENT_TYPE, HTTP.DEF_CONTENT_CHARSET.name());
		}
		try
		{
			this.execute(put, new VoidResponseHandler());
		}
		catch (HttpResponseException e)
		{
			if (e.getStatusCode() == HttpStatus.SC_EXPECTATION_FAILED)
			{
				// Retry with the Expect header removed
				put.removeHeaders(HTTP.EXPECT_DIRECTIVE);
				if (entity.isRepeatable())
				{
					this.execute(put, new VoidResponseHandler());
					return;
				}
			}
			throw e;
		}
	}

	@Override
	public void delete(String url) throws IOException
	{
		HttpDelete delete = new HttpDelete(url);
		this.execute(delete, new VoidResponseHandler());
	}

	@Override
	public void move(String sourceUrl, String destinationUrl) throws IOException
	{
		HttpMove move = new HttpMove(sourceUrl, destinationUrl);
		this.execute(move, new VoidResponseHandler());
	}

	@Override
	public void copy(String sourceUrl, String destinationUrl) throws IOException
	{
		HttpCopy copy = new HttpCopy(sourceUrl, destinationUrl);
		this.execute(copy, new VoidResponseHandler());
	}

	@Override
	public void createDirectory(String url) throws IOException
	{
		HttpMkCol mkcol = new HttpMkCol(url);
		this.execute(mkcol, new VoidResponseHandler());
	}

	@Override
	public boolean exists(String url) throws IOException
	{
		HttpHead head = new HttpHead(url);
		return this.execute(head, new ExistsResponseHandler());
	}

	/**
	 * Validate the response using the response handler. Aborts the request if there is an exception.
	 *
	 * @param <T>             Return type
	 * @param request		 Request to execute
	 * @param responseHandler Determines the return type.
	 * @return parsed response
	 */
	protected <T> T execute(HttpRequestBase request, ResponseHandler<T> responseHandler)
			throws IOException
	{
		try
		{
			// Clear circular redirect cache
			this.context.removeAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
			// Execute with response handler
			return this.client.execute(request, responseHandler, this.context);
		}
		catch (IOException e)
		{
			request.abort();
			throw e;
		}
	}

	/**
	 * No validation of the response. Aborts the request if there is an exception.
	 *
	 * @param request Request to execute
	 * @return The response to check the reply status code
	 */
	protected HttpResponse execute(HttpRequestBase request)
			throws IOException
	{
		try
		{
			// Clear circular redirect cache
			this.context.removeAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
			// Execute with no response handler
			return this.client.execute(request, this.context);
		}
		catch (IOException e)
		{
			request.abort();
			throw e;
		}
	}

	@Override
	public void shutdown()
	{
		this.client.getConnectionManager().shutdown();
	}

	/**
	 * Creates an AbstractHttpClient with all of the defaults.
	 */
	protected AbstractHttpClient createDefaultClient(ProxySelector selector)
	{
		SchemeRegistry schemeRegistry = this.createDefaultSchemeRegistry();
		ClientConnectionManager cm = this.createDefaultConnectionManager(schemeRegistry);
		HttpParams params = this.createDefaultHttpParams();
		AbstractHttpClient c = new DefaultHttpClient(cm, params);
		c.setRoutePlanner(this.createDefaultRoutePlanner(schemeRegistry, selector));
		return c;
	}

	/**
	 * Creates default params setting the user agent.
	 *
	 * @return Basic HTTP parameters with a custom user agent
	 */
	protected HttpParams createDefaultHttpParams()
	{
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		String version = Version.getSpecification();
		if (version == null)
		{
			version = VersionInfo.UNAVAILABLE;
		}
		HttpProtocolParams.setUserAgent(params, "Sardine/" + version);
		// Only selectively enable this for PUT but not all entity enclosing methods
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.DEF_CONTENT_CHARSET.name());

		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpConnectionParams.setSocketBufferSize(params, 8192);
		return params;
	}

    /**
     * Creates a new {@link org.apache.http.conn.scheme.SchemeRegistry} for
     * default ports with socket factories.
     *
     * @return a new {@link org.apache.http.conn.scheme.SchemeRegistry}.
     */
    protected SchemeRegistry createDefaultSchemeRegistry() {
        SchemeRegistry registry = null;
        try {
            registry = new SchemeRegistry();
            registry.register(new Scheme("http", 80, this.createDefaultSocketFactory()));
            registry.register(new Scheme("https", 443, this.createDefaultSecureSocketFactory()));

//            KeyStore localKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            localKeyStore.load(null, null);
//            MySSLSocketFactory localMySSLSocketFactory1 = new MySSLSocketFactory(localKeyStore);
//            localMySSLSocketFactory1.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
////            +++++++++++
//             X509TrustManager tm = new X509TrustManager() { 
//            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//            }
//
//            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
//            }
//
//            public X509Certificate[] getAcceptedIssuers() {
//                return null;
//            }
//        };
//        SSLContext ctx = SSLContext.getInstance("TLS");
//        ctx.init(null, new TrustManager[]{tm}, null);
//        SSLSocketFactory ssf = new MySSLSocketFactory(ctx);
//        ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            registry.register(new Scheme("https", ssf, 443));
//            //            +++++++++++
            return registry;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return registry;
    }

	/**
	 * @return Default socket factory
	 */
	protected PlainSocketFactory createDefaultSocketFactory()
	{
		return PlainSocketFactory.getSocketFactory();
	}

	/**
	 * @return Default SSL socket factory
	 */
	protected SSLSocketFactory createDefaultSecureSocketFactory()
	{
		return SSLSocketFactory.getSocketFactory();
	}

	/**
	 * Use fail fast connection manager when connections are not released properly.
	 *
	 * @param schemeRegistry Protocol registry
	 * @return Default connection manager
	 */
	protected ClientConnectionManager createDefaultConnectionManager(SchemeRegistry schemeRegistry)
	{
		return new PoolingClientConnectionManager(schemeRegistry);
	}

	/**
	 * Override to provide proxy configuration
	 *
	 * @param schemeRegistry Protocol registry
	 * @param selector	   Proxy configuration
	 * @return ProxySelectorRoutePlanner configured with schemeRegistry and selector
	 */
	protected HttpRoutePlanner createDefaultRoutePlanner(SchemeRegistry schemeRegistry, ProxySelector selector)
	{
		return new ProxySelectorRoutePlanner(schemeRegistry, selector);
	}

}