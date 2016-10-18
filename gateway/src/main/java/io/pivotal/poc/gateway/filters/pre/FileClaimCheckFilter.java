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

import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.HttpServletRequestWrapper;

public class FileClaimCheckFilter extends ZuulFilter {

	private static Logger log = LoggerFactory.getLogger(FileClaimCheckFilter.class);

	private final Field requestField;

	public FileClaimCheckFilter() {
		this.requestField = ReflectionUtils.findField(HttpServletRequestWrapper.class, "req", HttpServletRequest.class);
		Assert.notNull(this.requestField, "HttpServletRequestWrapper.req field not found");
		this.requestField.setAccessible(true);
	}

	@Override
	public String filterType() {
		return "route";
	}

	@Override
	public int filterOrder() {
		return 1;
	}

	@Override
	public boolean shouldFilter() {
		RequestContext ctx = RequestContext.getCurrentContext();
		StandardMultipartHttpServletRequest multipartRequest = this.exractMultipartRequest(ctx.getRequest());
		return multipartRequest != null;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		StandardMultipartHttpServletRequest multipartRequest = this.exractMultipartRequest(ctx.getRequest());
		System.out.println("file map: " + multipartRequest.getFileMap());
		MultipartFile file = multipartRequest.getFile("file");
		try {
			System.out.println("file content as string: " + new String(file.getBytes()));
			String name = multipartRequest.getParameter("name");
			String uploadedFilePath = String.format("/tmp/orders/%s", name);
			file.transferTo(new File(uploadedFilePath));
			ctx.getRequest().setAttribute("uploaded-file-path", uploadedFilePath);
			RestTemplate template = new RestTemplate();
			String url = "http://localhost:8080/messages/orders";
			RequestEntity<String> requestEntity = RequestEntity.post(new URI(url)).contentType(MediaType.TEXT_PLAIN).body(uploadedFilePath);
			template.exchange(requestEntity, String.class);
			ctx.setResponseStatusCode(200);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		log.info(String.format("%s request to %s", multipartRequest.getMethod(), multipartRequest.getRequestURL().toString()));
		return null;
	}

	private StandardMultipartHttpServletRequest exractMultipartRequest(HttpServletRequest request) {
		while (request != null) {
			log.info("request class: {}", request.getClass());
			if (StandardMultipartHttpServletRequest.class.isAssignableFrom(request.getClass())) {
				return (StandardMultipartHttpServletRequest) request;
			}
			try {
				request = (HttpServletRequest) ReflectionUtils.getField(this.requestField, request);
			}
			catch (Exception e) {
				break;
			}
		}
		return null;
	}
}
