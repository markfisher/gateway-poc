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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SimpleIdGenerator;

import io.pivotal.poc.claimcheck.ClaimCheckStore;
import io.pivotal.poc.claimcheck.LocalFileClaimCheckStore;

/**
 * @author Mark Fisher
 */
@Configuration
@EnableBinding(Processor.class)
@EnableConfigurationProperties(XsltProcessorConfigurationProperties.class)
public class XsltProcessorConfiguration {

	private static Pattern CLAIM_CHECK_PATTERN = Pattern.compile("^.*?:\\s*\"([-0-9a-f]+)\"}$");

	@Autowired
	private XsltProcessorConfigurationProperties properties;

	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public Message<?> process(Message<?> message) {
		String payload = message.getPayload().toString();
		try {
			// simulating processing time
			Thread.sleep(10_000);
		}
		catch (Exception e) {
			Thread.currentThread().interrupt();
		}
		Matcher matcher = CLAIM_CHECK_PATTERN.matcher(payload);
		if (matcher.matches()) {
			String claimCheck = matcher.group(1);
			Resource resource = claimCheckStore().find(claimCheck);
			try {
				payload = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
				message = MessageBuilder.withPayload(payload)
						.copyHeaders(message.getHeaders())
						.setHeader("claimCheck", claimCheck)
						.build();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return transformer().transform(message);
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
		return new LocalFileClaimCheckStore(dir, new SimpleIdGenerator());
	}
}
