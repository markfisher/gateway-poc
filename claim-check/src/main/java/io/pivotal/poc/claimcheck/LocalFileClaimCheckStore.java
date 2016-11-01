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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.IdGenerator;

/**
 * @author Mark Fisher
 */
public class LocalFileClaimCheckStore implements FileClaimCheckStore {

	private static final Logger log = LoggerFactory.getLogger(LocalFileClaimCheckStore.class);

	private final File directory;

	private final IdGenerator idGenerator;

	public LocalFileClaimCheckStore(File directory, IdGenerator idGenerator) {
		Assert.notNull(directory, "directory must not be null");
		Assert.notNull(idGenerator, "IdGenerator must not be null");
		if (directory.exists()) {
			Assert.isTrue(directory.isDirectory());
		}
		else {
			directory.mkdirs();
		}
		this.directory = directory;
		this.idGenerator = idGenerator;
	}

	@Override
	public Resource find(String id) {
		return new FileSystemResource(new File(this.directory, id));
	}

	@Override
	public String save(Resource resource) {
		String id = idGenerator.generateId().toString();
		try {
			File outputFile = new File(this.directory, id);
			FileCopyUtils.copy(new InputStreamReader(resource.getInputStream()), new FileWriter(outputFile));
			log.info("saved '{}' to {}", id, outputFile.getAbsolutePath());
		}
		catch (IOException e) {
			throw new IllegalStateException("failed to write file", e);
		}
		return id;
	}
}
