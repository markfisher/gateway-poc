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

package org.springframework.cloud.stream.processor.xslt;

import static org.junit.Assert.assertEquals;

import java.io.FileReader;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

/**
 * @author Mark Fisher
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties="xslt.stylesheet=test.xsl")
public class XsltProcessorApplicationTests {

	@Autowired
	private Processor processor;

	@Autowired
	private MessageCollector collector;

	@Test
	public void test() throws Exception {
		String expected = FileCopyUtils.copyToString(new FileReader(ResourceUtils.getFile("classpath:test.html")));
		String payload = FileCopyUtils.copyToString(new FileReader(ResourceUtils.getFile("classpath:test.xml")));
		Message<String> input = MessageBuilder.withPayload(payload).build();
		processor.input().send(input);
		Message<?> output = collector.forChannel(processor.output()).take();
		assertEquals(expected, output.getPayload());
	}
}
