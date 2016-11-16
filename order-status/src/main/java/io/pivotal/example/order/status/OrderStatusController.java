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

package io.pivotal.example.order.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mark Fisher
 */
@RestController
public class OrderStatusController {

	private static Logger log = LoggerFactory.getLogger(OrderStatusController.class);

	private static final Pattern PATTERN = Pattern.compile("^.*?id=\"([-0-9a-z]+)\".*?price=\"(.*?)\".*$");

	private final ConcurrentMap<String, List<String>> statusMap = new ConcurrentHashMap<>();

	@RequestMapping(value = "/status/{orderId}")
	public String status(@PathVariable String orderId) {
		List<String> status = statusMap.get(orderId);
		return (status == null) ? "unknown" : StringUtils.collectionToCommaDelimitedString(status);
	}

	@StreamListener(Sink.INPUT)
	public void receive(Message<?> message) {
		String payload = message.getPayload().toString();
		Matcher matcher = PATTERN.matcher(payload.replace('\n', ' '));
		if (matcher.matches()) {
			String id = matcher.group(1);
			String price = matcher.group(2);
			log.info("updating order status for order ID '{}' with price: {}", id, price);
			statusMap.putIfAbsent(id, new ArrayList<String>());
			if (price.contains(".")) {
				statusMap.get(id).add("taxed");
			}
			else {
				statusMap.get(id).add("priced");
			}
		}
	}
}
