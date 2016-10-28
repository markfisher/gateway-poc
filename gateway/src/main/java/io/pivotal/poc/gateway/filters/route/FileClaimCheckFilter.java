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
 
package io.pivotal.poc.gateway.filters.route;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.BodyBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import com.netflix.zuul.context.RequestContext;

public class FileClaimCheckFilter extends AbstractRouteFilter {

	public FileClaimCheckFilter() {
		super(2);
	}

	@Override
	public boolean shouldFilter(RequestContext ctx) {
		StandardMultipartHttpServletRequest multipartRequest = this.extractMultipartRequest(ctx.getRequest());
		return multipartRequest != null;
	}

	@Override
	public void filter(RequestContext ctx) {
		StandardMultipartHttpServletRequest multipartRequest = this.extractMultipartRequest(ctx.getRequest());
		log.info("file map: {}", multipartRequest.getFileMap());
		MultipartFile file = multipartRequest.getFile("file");
		try {
			log.info("file content as string: {}", new String(file.getBytes()));
			String name = multipartRequest.getParameter("name");
			String uploadedFilePath = String.format("/tmp/orders/%s", name);
			file.transferTo(new File(uploadedFilePath));
			RestTemplate template = new RestTemplate();
			String url = "http://localhost:8080/messages/orders";
			BodyBuilder builder = RequestEntity.post(new URI(url));
			for (Map.Entry<String, String> header : ctx.getZuulRequestHeaders().entrySet()) {
				log.info("adding header: {}={}", header.getKey(), header.getValue());
				builder.header(header.getKey(), header.getValue());
			}
			builder.contentType(MediaType.TEXT_PLAIN);
			RequestEntity<String> requestEntity = builder.body(uploadedFilePath);
			template.exchange(requestEntity, String.class);
			ctx.setResponseStatusCode(200);
			ctx.setResponseBody("Received: " + name);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		log.info(String.format("%s request to %s", multipartRequest.getMethod(), multipartRequest.getRequestURL().toString()));
	}
}
