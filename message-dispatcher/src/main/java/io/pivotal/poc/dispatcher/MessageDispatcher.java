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

import java.util.Collections;

import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Controller;
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

	private final BinderAwareChannelResolver resolver;

	public MessageDispatcher(BinderAwareChannelResolver resolver) {
		this.resolver = resolver;
	}

	@RequestMapping(path = "/{topic}", method = RequestMethod.POST, consumes = {"text/*", "application/json"})
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ResponseEntity<?> handleRequest(@PathVariable String topic, @RequestBody String body, @RequestHeader(HttpHeaders.CONTENT_TYPE) Object contentType) {
		sendMessage(topic, body, contentType);
		return ResponseEntity.ok("Received:\n" + body);
	}

//	@RequestMapping(path = "/{topic}", method = RequestMethod.POST, consumes = "*/*")
//	@ResponseStatus(HttpStatus.ACCEPTED)
//	public void handleRequest(@PathVariable String topic, @RequestBody byte[] body, @RequestHeader(HttpHeaders.CONTENT_TYPE) Object contentType) {
//		sendMessage(topic, body, contentType);
//	}

	private void sendMessage(String topic, Object body, Object contentType) {
		MessageChannel channel = resolver.resolveDestination(topic + ".input");
		channel.send(MessageBuilder.createMessage(body,
				new MessageHeaders(Collections.singletonMap(MessageHeaders.CONTENT_TYPE, contentType))));
	}
}
