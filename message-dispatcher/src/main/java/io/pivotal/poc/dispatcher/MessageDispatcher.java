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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Mark Fisher
 */
@Controller
public class MessageDispatcher {

	private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

	private final BinderAwareChannelResolver resolver;

	private final Set<String> requestHeadersToMap;

	private final IdGenerator idGenerator = new JdkIdGenerator();

	public MessageDispatcher(BinderAwareChannelResolver resolver, Set<String> requestHeadersToMap) {
		this.resolver = resolver;
		this.requestHeadersToMap = requestHeadersToMap;
	}

	@RequestMapping(path = "/{topic}", method = RequestMethod.POST, consumes = {"text/*", "application/json", "application/x-claimcheck"})
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<?> handleRequest(@PathVariable String topic, @RequestBody String body, @RequestHeader HttpHeaders requestHeaders) {
		log.info("received request with body '" + body + "' and headers: " + requestHeaders);
		String id = sendMessage(topic, body, requestHeaders);
		return ResponseEntity.ok(id);
	}

	private String sendMessage(String topic, Object body, HttpHeaders requestHeaders) {
		MessageChannel channel = resolver.resolveDestination(topic + ".input");
		MessageBuilder<?> builder = MessageBuilder.withPayload(body);
		builder.setHeader(MessageHeaders.CONTENT_TYPE, requestHeaders.getContentType());
		for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
			String headerName = entry.getKey();
			if (requestHeadersToMap.contains(headerName)) {
				builder.setHeaderIfAbsent(headerName, StringUtils.collectionToCommaDelimitedString(entry.getValue()));
			}
		}
		String idKey = (topic.endsWith("s") ? topic.substring(0, topic.length() - 1) : topic) + "Id";
		String id = this.idGenerator.generateId().toString();
		builder.setHeader(idKey, id);
		Message<?> message = builder.build();
		channel.send(message);
		log.info("dispatched message with {}: {}", idKey, id);
		return id;
	}
}
