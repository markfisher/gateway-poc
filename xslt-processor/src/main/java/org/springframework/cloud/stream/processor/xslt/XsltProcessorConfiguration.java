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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.xml.transformer.XsltPayloadTransformer;
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

	@Autowired
	private XsltProcessorConfigurationProperties properties;

	@Bean
	public XsltProcessor xsltProcessor() { 
		XsltProcessor processor = new XsltProcessor(transformer(), properties.getDelay());
		processor.setStylesheetName(properties.getStylesheet().getFilename());
		return processor;
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

	@Bean
	public ClaimCheckMessageConverter claimCheckMessageConverter(ClaimCheckStore claimCheckStore) {
		return new ClaimCheckMessageConverter(claimCheckStore);
	}
}
