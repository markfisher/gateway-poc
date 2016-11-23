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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.JdkIdGenerator;

import io.pivotal.poc.claimcheck.ClaimCheckStore;
import io.pivotal.poc.claimcheck.LocalFileClaimCheckStore;

/**
 * @author Mark Fisher
 */
@Configuration
@EnableBinding(Processor.class)
@EnableConfigurationProperties(XsltProcessorConfigurationProperties.class)
public class XsltProcessorConfiguration {

	private static Logger log = LoggerFactory.getLogger(XsltProcessorConfiguration.class);

	@Autowired
	private XsltProcessorConfigurationProperties properties;

	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public Message<?> process(String payload, @Headers MessageHeaders headers) {
		log.info("received payload: {}", payload);
		log.info("received headers: {}", headers);
		try {
			// simulating processing time
			Thread.sleep(10_000);
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}
		boolean isClaimCheck = "application/x-claimcheck".equals(headers.get("contentType"));
		Message<String> message = isClaimCheck ? checkout(payload, headers)
				: MessageBuilder.withPayload(payload).copyHeaders(headers).build();
		Message<?> transformed = transformer().transform(message);
		return isClaimCheck ? checkin(transformed) : transformed;
	}

	private Message<String> checkout(String claimCheck, MessageHeaders headers) {
		log.info("checking out {}", claimCheck);
		Resource resource = claimCheckStore().find(claimCheck);
		try {
			String payload = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
			return MessageBuilder.withPayload(payload)
					.copyHeaders(headers)
					.setHeader("claimCheck", claimCheck)
					.setHeader("stylesheet", properties.getStylesheet().getFilename())
					.build();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private Message<String> checkin(Message<?> message) {
		String id = message.getHeaders().get("claimCheck", String.class);
		Assert.hasText(id, "expected a 'claimCheck' header");
		claimCheckStore().update(id, new ByteArrayResource(message.getPayload().toString().getBytes()));
		log.info("checked in {}", id);
		return MessageBuilder.withPayload(id).copyHeaders(message.getHeaders()).build();
	}

	@Bean
	@RefreshScope
	public XsltPayloadTransformer transformer() {
		XsltPayloadTransformer transformer = new XsltPayloadTransformer(properties.getStylesheet());
		transformer.setXsltParamHeaders(properties.getParamHeaders());
		return transformer;
	}

	@Bean
	public ClaimCheckStore claimCheckStore() {
		File dir = new File("/tmp/uploads");
		return new LocalFileClaimCheckStore(dir, new JdkIdGenerator());
	}
}
