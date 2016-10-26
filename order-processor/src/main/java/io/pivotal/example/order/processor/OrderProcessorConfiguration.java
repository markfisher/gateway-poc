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

package io.pivotal.example.order.processor;

import java.io.File;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.IdGenerator;
import org.springframework.util.SimpleIdGenerator;

import io.pivotal.poc.claimcheck.FileClaimCheckStore;
import io.pivotal.poc.claimcheck.LocalFileClaimCheckStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableBinding(Processor.class)
@EnableConfigurationProperties(OrderProcessorConfigurationProperties.class)
public class OrderProcessorConfiguration {

	private static Logger log = LoggerFactory.getLogger(OrderProcessorConfiguration.class);

	@Autowired
	private OrderProcessorConfigurationProperties properties;

	@Autowired
	private FileClaimCheckStore fileClaimCheckStore;

	@Bean
	public ThreadPoolTaskScheduler scheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	public IdGenerator idGenerator() {
		return new SimpleIdGenerator();
	}

	@Bean
	public FileClaimCheckStore fileClaimCheckStore() {
		return new LocalFileClaimCheckStore(properties.getDirectory(), idGenerator());
	}

	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public Message<String> process(@Payload Map<String, String> payload, @Headers Map<String, String> headers) throws Exception {
		log.info("received order with payload: {}, and headers: {}", payload, headers);
		String orderFileId = payload.get("order");
		Resource orderResource = fileClaimCheckStore.find(orderFileId);
		File orderFile = orderResource.getFile();
		File pendingFile = new File(orderFile.getParentFile(), String.format("%s.pending", orderFile.getName()));
		orderFile.renameTo(pendingFile);
		Thread.sleep(1_000);
		log.info("processing order: {}", pendingFile);
		String filename = pendingFile.getName().substring(0, pendingFile.getName().lastIndexOf('.'));
		File dest = new File(properties.getDirectory(), String.format("%s.phase2", filename));
		pendingFile.renameTo(dest);
		return MessageBuilder.withPayload(orderFile.getAbsolutePath()).copyHeaders(headers).build();
	}
}
