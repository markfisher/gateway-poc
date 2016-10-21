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

package io.pivotal.example.order.phase2;

import java.io.File;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Marius Bogoevici
 */
@Configuration
@EnableBinding(Sink.class)
@EnableConfigurationProperties(OrderProcessorPhase2ConfigurationProperties.class)
public class OrderProcessorPhase2Configuration {


	private static Logger log = LoggerFactory.getLogger(OrderProcessorPhase2Configuration.class);

	@Autowired
	private OrderProcessorPhase2ConfigurationProperties properties;

	@Bean
	public ThreadPoolTaskScheduler scheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@StreamListener(Processor.INPUT)
	public void process(String orderPath) throws Exception {
		log.info("received order: {}", orderPath);
		File phase2Waiting = new File(String.format("%s.phase2", orderPath));
		scheduler().schedule(new OrderProcessor(phase2Waiting), new Date(System.currentTimeMillis() + 30_000));
	}

	private class OrderProcessor implements Runnable {

		private final File pendingFile;

		private OrderProcessor(File pendingFile) {
			this.pendingFile = pendingFile;
		}

		@Override
		public void run() {
			log.info("processing order: {}", pendingFile);
			String filename = pendingFile.getName().substring(0, pendingFile.getName().lastIndexOf('.'));
			File dest = new File(properties.getDirectory(),String.format("%s.complete", filename));
			pendingFile.renameTo(dest);
		}
	}
}
