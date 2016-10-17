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

package io.pivotal.poc.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * @author Mark Fisher
 */
public class FunctionExecutorPoolManager {

	private final AppDeployer deployer;

	private final ResourceLoader resourceLoader;

	public FunctionExecutorPoolManager(AppDeployer deployer, ResourceLoader resourceLoader) {
		this.deployer = deployer;
		this.resourceLoader = resourceLoader;
	}

	public void addStreamApp(String name) {
		Map<String, String> properties = new HashMap<>();
		properties.put("function.name", name);
		properties.put("spring.cloud.stream.bindings.input.destination", name + ".input");
		properties.put("spring.cloud.stream.bindings.output.destination", name + ".output");
		AppDefinition definition = new AppDefinition(name + UUID.randomUUID(), properties);
		String uri = "maven://org.springframework.cloud:spring-cloud-function-stream:1.0.0.BUILD-SNAPSHOT";
		Resource resource = this.resourceLoader.getResource(uri);
		Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, "streaming");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, deploymentProperties);
		deployer.deploy(request);
	}
}
