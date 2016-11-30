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

package io.pivotal.poc.claimcheck;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ClaimCheckResource extends AbstractResource {

	private final String claimCheck;

	private final Resource delegate;

	public ClaimCheckResource(String claimCheck, Resource delegate) {
		Assert.hasText(claimCheck, "a non-empty claimCheck is required");
		Assert.notNull(delegate, "delegate Resource must not be null");
		this.claimCheck = claimCheck;
		this.delegate = delegate;
	}

	public String getClaimCheck() {
		return this.claimCheck;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.delegate.getInputStream();
	}

	@Override
	public String getDescription() {
		return "claimCheck [" + this.claimCheck + "]";
	}

	@Override
	public String toString() {
		return this.getDescription();
	}
}
