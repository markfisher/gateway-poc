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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.FileCopyUtils;

@Configuration
@EnableBinding(Sink.class)
@EnableConfigurationProperties(OrderProcessorConfigurationProperties.class)
public class OrderProcessorConfiguration {

	private static Logger log = LoggerFactory.getLogger(OrderProcessorConfiguration.class);

	@Autowired
	private OrderProcessorConfigurationProperties properties;

	@Bean
	public ThreadPoolTaskScheduler scheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@StreamListener(Sink.INPUT)
	public void process(String order) {
		log.info("received order: {}", order);
		File file = new File(properties.getDirectory(), String.format("%s.pending", order));
		try {
			FileCopyUtils.copy(String.format("%s received at %d", order, System.currentTimeMillis()), new FileWriter(file));
			scheduler().schedule(new OrderProcessor(order), new Date(System.currentTimeMillis() + 60_000));
		}
		catch (IOException e) {
			log.error("failed to process order", e);
		}
	}

	private class OrderProcessor implements Runnable {

		private final String order;

		private OrderProcessor(String order) {
			this.order = order;
		}

		@Override
		public void run() {
			log.info("processing order: {}", order);
			File file = new File(properties.getDirectory(), String.format("%s.pending", order));
			File dest = new File(properties.getDirectory(), String.format("%s.complete", order));
			file.renameTo(dest);
		}
	}
}
