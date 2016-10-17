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

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Mark Fisher
 */
@RestController
@EnableConfigurationProperties(OrderStatusConfigurationProperties.class)
public class OrderStatusController {

	@Autowired
	private OrderStatusConfigurationProperties properties;

	@RequestMapping(value = "/status/{orderId}")
	public String status(@PathVariable String orderId) {
		for (File file : properties.getDirectory().listFiles(f -> f.getName().startsWith(orderId))) {
			int lastdot = file.getName().lastIndexOf('.');
			if (lastdot != -1) {
				return file.getName().substring(lastdot + 1);
			}
		}
		return "unknown";
	}
}
