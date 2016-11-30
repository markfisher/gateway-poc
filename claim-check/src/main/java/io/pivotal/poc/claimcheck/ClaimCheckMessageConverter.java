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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * @author Mark Fisher
 */
public class ClaimCheckMessageConverter extends AbstractMessageConverter {

	private static Logger log = LoggerFactory.getLogger(ClaimCheckMessageConverter.class);

	private final ClaimCheckStore claimCheckStore;

	public ClaimCheckMessageConverter(ClaimCheckStore claimCheckStore) {
		super(new MimeType("application", "x-claimcheck"));
		Assert.notNull(claimCheckStore, "ClaimCheckStore must not be null");
		this.claimCheckStore = claimCheckStore;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return Resource.class.isAssignableFrom(clazz) || String.class.equals(clazz);
	}

	@Override
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
		if (targetClass.equals(Resource.class)) {
			if ("application/x-claimcheck".equals(message.getHeaders().get(MessageHeaders.CONTENT_TYPE))) {
				Object resource = this.claimCheckStore.find(message.getPayload().toString());
				log.info("checked out {}", resource);
				return resource;
			}
			else if (message.getPayload() instanceof String) {
				return new ByteArrayResource(message.getPayload().toString().getBytes());
			}
		}
		return null;
	}

	@Override
	protected Object convertToInternal(Object payload, MessageHeaders headers, Object conversionHint) {
		Resource resource = new ByteArrayResource(payload.toString().getBytes());
		String claimCheck = headers.get("claimCheck", String.class);
		if (claimCheck == null) {
			claimCheck = this.claimCheckStore.save(resource);
		}
		else {
			this.claimCheckStore.update(claimCheck, resource);
		}
		log.info("checked in {}", claimCheck);
		return claimCheck;
	}
}
