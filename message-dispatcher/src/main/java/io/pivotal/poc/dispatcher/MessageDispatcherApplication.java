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

package io.pivotal.poc.dispatcher;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@EnableBinding
@EnableEurekaClient
@SpringBootApplication
@EnableConfigurationProperties(HeaderMappingConfigurationProperties.class)
public class MessageDispatcherApplication {

	@Autowired
	private HeaderMappingConfigurationProperties headerMappingConfigurationProperties;

	public static void main(String[] args) {
		SpringApplication.run(MessageDispatcherApplication.class, args);
	}

	@Bean
	public MessageDispatcher messageDispatcher(BinderAwareChannelResolver resolver) {
		Set<String> requestHeadersToMap = StringUtils.commaDelimitedListToSet(headerMappingConfigurationProperties.getRequest());
		return new MessageDispatcher(resolver, requestHeadersToMap);
	}
}
