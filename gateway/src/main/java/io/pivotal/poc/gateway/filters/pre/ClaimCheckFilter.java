/*
 * Copyright 2016 the original author or authors.
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
 
package io.pivotal.poc.gateway.filters.pre;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;
import com.netflix.zuul.http.ServletInputStreamWrapper;

import io.pivotal.poc.claimcheck.ClaimCheckStore;

/**
 * @author Mark Fisher
 */
public class ClaimCheckFilter extends AbstractPreFilter {

	private final ClaimCheckStore store;

	private final int threshold;

	private Field requestField;

	private Field servletRequestField;

	private Field contentDataField;
	
	public ClaimCheckFilter(ClaimCheckStore store, int threshold) {
		super(2);
		Assert.notNull(store, "FileClaimCheckStore must not be null");
		this.store = store;
		this.threshold = threshold;
		this.requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "req", HttpServletRequest.class);
		this.servletRequestField = ReflectionUtils.findField(ServletRequestWrapper.class, "request", ServletRequest.class);
		this.contentDataField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "contentData", byte[].class);
		Assert.notNull(this.requestField, "HttpServletRequestWrapper.req field not found");
		Assert.notNull(this.servletRequestField, "ServletRequestWrapper.request field not found");
		Assert.notNull(this.contentDataField, "HttpServletRequestWrapper.contentData field not found");
		this.requestField.setAccessible(true);
		this.servletRequestField.setAccessible(true);
		this.contentDataField.setAccessible(true);
	}

	@Override
	public boolean shouldFilter(RequestContext ctx) {
		return ctx.getRequest().getContentLength() > this.threshold;
	}

	@Override
	public void filter(RequestContext ctx) {
		HttpServletRequest request = ctx.getRequest();
		try {
			InputStream inputStream = request.getInputStream();
			Resource resource = new InputStreamResource(inputStream);
			String id = this.store.save(resource);
			FileClaimCheckRequestWrapper wrapper = null;
			if (request instanceof HttpServletRequestWrapper) {
				HttpServletRequest wrapped = (HttpServletRequest) ReflectionUtils.getField(this.requestField, request);
				if (wrapped instanceof HttpServletRequestWrapper) {
					wrapped = ((HttpServletRequestWrapper) wrapped).getRequest();
				}
				wrapper = new FileClaimCheckRequestWrapper(id, wrapped);
				ReflectionUtils.setField(this.requestField, request, wrapper);
				ReflectionUtils.setField(this.contentDataField, request, id.getBytes());
				if (request instanceof ServletRequestWrapper) {
					ReflectionUtils.setField(this.servletRequestField, request, wrapper);
				}
			}
			else {
				wrapper = new FileClaimCheckRequestWrapper(id, request);
				ctx.setRequest(wrapper);
			}
			if (wrapper != null) {
				ctx.getZuulRequestHeaders().put("content-type", wrapper.getContentType());
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		log.info(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
	}

	private class FileClaimCheckRequestWrapper extends Servlet30RequestWrapper {

		private final byte[] contentData;

		public FileClaimCheckRequestWrapper(String id, HttpServletRequest request) {
			super(request);
			this.contentData = id.getBytes();
		}

		@Override
		public final byte[] getContentData() {
			return this.contentData;
		}

		@Override
		public final String getContentType() {
			return "application/x-claimcheck";
		}

		@Override
		public final int getContentLength() {
			return this.contentData.length;
		}

		@Override
		public final long getContentLengthLong() {
			return getContentLength();
		}

		@Override
		public final ServletInputStream getInputStream() throws IOException {
			return new ServletInputStreamWrapper(this.contentData);
		}
	}

	private class Servlet30RequestWrapper extends HttpServletRequestWrapper {

		private HttpServletRequest request;

		Servlet30RequestWrapper(HttpServletRequest request) {
			super(request);
			this.request = request;
		}

		/**
		 * There is a bug in zuul 1.2.2 where HttpServletRequestWrapper.getRequest returns a wrapped request rather than the raw one.
		 * @return the original HttpServletRequest
		 */
		@Override
		public HttpServletRequest getRequest() {
			return this.request;
		}
	}
}
