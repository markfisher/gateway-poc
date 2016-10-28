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

package io.pivotal.poc.gateway.filters;

import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;

/**
 * @author Mark Fisher
 */
public abstract class AbstractFilter extends ZuulFilter {

	protected static Logger log = LoggerFactory.getLogger(AbstractFilter.class);

	private static final Field requestField;

	static {
		requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "req", HttpServletRequest.class);
		requestField.setAccessible(true);
	}

	private final int order;

	protected AbstractFilter(int order) {
		this.order = order;
	}

	@Override
	public final int filterOrder() {
		return this.order;
	}

	@Override
	public final boolean shouldFilter() {
		return this.shouldFilter(RequestContext.getCurrentContext());
	}

	@Override
	public Object run() {
		this.filter(RequestContext.getCurrentContext());
		return null;
	}

	protected boolean shouldFilter(RequestContext requestContext) {
		return true;
	}

	protected abstract void filter(RequestContext requestContext);

	protected StandardMultipartHttpServletRequest extractMultipartRequest(HttpServletRequest request) {
		while (request != null) {
			log.info("request class: {}", request.getClass());
			if (StandardMultipartHttpServletRequest.class.isAssignableFrom(request.getClass())) {
				return (StandardMultipartHttpServletRequest) request;
			}
			try {
				request = (HttpServletRequest) ReflectionUtils.getField(requestField, request);
			}
			catch (Exception e) {
				break;
			}
		}
		return null;
	}
}
