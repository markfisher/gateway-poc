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

package org.springframework.cloud.stream.processor.xslt;

import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.core.io.Resource;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * @author Mark Fisher
 */
public class XsltProcessor {

	private static Logger log = LoggerFactory.getLogger(XsltProcessor.class);

	private volatile String stylesheetName;

	private final XsltPayloadTransformer transformer;

	private final long delay;

	public XsltProcessor(XsltPayloadTransformer transformer, long delay) {
		Assert.notNull(transformer, "XsltPayloadTransformer must not be null");
		this.transformer = transformer;
		this.delay = delay;
	}

	public void setStylesheetName(String stylesheetName) {
		this.stylesheetName = stylesheetName;
	}

	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public Message<?> process(Resource resource, @Headers MessageHeaders headers) throws Exception {
		log.info("received payload: {}", resource);
		log.info("received headers: {}", headers);
		try {
			// simulating processing time
			Thread.sleep(this.delay);
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}
		String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		MessageBuilder<String> builder = MessageBuilder.withPayload(content)
				.copyHeaders(headers)
				.setHeader("stylesheet", this.stylesheetName);
		return this.transformer.transform(builder.build());
	}
}
