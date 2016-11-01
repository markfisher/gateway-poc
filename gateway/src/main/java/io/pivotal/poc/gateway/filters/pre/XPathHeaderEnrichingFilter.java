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

import java.util.Map;

import org.w3c.dom.Node;

import org.springframework.integration.xml.DefaultXmlPayloadConverter;
import org.springframework.integration.xml.XmlPayloadConverter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;

import com.netflix.zuul.context.RequestContext;

/**
 * @author Mark Fisher
 */
public class XPathHeaderEnrichingFilter extends AbstractPreFilter {

	private final String key;

	private final XPathExpression expression;

	private final XmlPayloadConverter converter = new DefaultXmlPayloadConverter();

	public XPathHeaderEnrichingFilter(String key, String xpathExpression) {
		super(1);
		this.key = key;
		this.expression = XPathExpressionFactory.createXPathExpression(xpathExpression);
	}

	@Override
	protected boolean shouldFilter(RequestContext requestContext) {
		return null != this.extractMultipartRequest(requestContext.getRequest());
	}

	@Override
	protected void filter(RequestContext requestContext) {
		StandardMultipartHttpServletRequest multipartRequest = this.extractMultipartRequest(requestContext.getRequest());
		try {
			for (Map.Entry<String, MultipartFile> entry : multipartRequest.getFileMap().entrySet()) {
				String value = this.evaluateExpression(entry.getValue().getInputStream());
				if (value != null) {
					String headerName = String.format("%s.%s", entry.getKey(), key);
					log.info("adding header from xpath evaluation: {}={}", headerName, value);
					requestContext.getZuulRequestHeaders().put(headerName, value);
				}
			}
		}
		catch (Exception e) {
			log.error("xpath header enrichment failed", e); 
		}
	}

	private String evaluateExpression(Object source) throws Exception {
		Node node = converter.convertToNode(source);
		return this.expression.evaluateAsString(node);
	}
}
