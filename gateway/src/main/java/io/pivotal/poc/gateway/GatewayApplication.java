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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import io.pivotal.poc.gateway.filters.pre.FileClaimCheckFilter;
import io.pivotal.poc.gateway.filters.pre.RequestMonitoringFilter;

@EnableZuulProxy
@EnableBinding
@EnableEurekaClient
@EnableConfigurationProperties(LocalDeployerProperties.class)
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

	@Bean
	public AppDeployer appDeployer(LocalDeployerProperties properties) {
		return new LocalAppDeployer(properties);
	}

	@Bean
	public MavenResourceLoader mavenResourceLoader() {
		return new MavenResourceLoader(new MavenProperties());
	}

	@Bean
	public FunctionExecutorPoolManager functionExecutorPoolManager(AppDeployer deployer) {
		return new FunctionExecutorPoolManager(deployer, mavenResourceLoader());
	}

	@Bean
	public MultipartResolver multipartResolver() {
		return new StandardServletMultipartResolver();
	}

	@Bean
	public FileClaimCheckFilter fileClaimCheckFilter() {
		return new FileClaimCheckFilter();
	}

	@Bean
	public RequestMonitoringFilter requestMonitoringFilter(FunctionExecutorPoolManager poolManager) {
		return new RequestMonitoringFilter(poolManager);
	}
}
