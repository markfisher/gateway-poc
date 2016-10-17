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
 
package io.pivotal.poc.gateway.filters.pre;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import io.pivotal.poc.gateway.FunctionExecutorPoolManager;

public class RequestMonitoringFilter extends ZuulFilter {

	private final FunctionExecutorPoolManager poolManager;

	private final AtomicInteger lastSize = new AtomicInteger(0);

	private final ConcurrentMap<String, Window> hits = new ConcurrentHashMap<>();

	public RequestMonitoringFilter(FunctionExecutorPoolManager poolManager) {
		this.poolManager = poolManager;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Override
	public int filterOrder() {
		return 2;
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		String url = request.getRequestURL().toString();
		if ("POST".equalsIgnoreCase(request.getMethod()) && url.contains("/messages/")) {
			String topic = url.substring(url.lastIndexOf('/') + 1);
			hits.putIfAbsent(topic, new Window());
			int size = hits.get(topic).increment();
			System.out.println("topic [" + topic + "] hits in last 10 seconds: " + size + "(last: " + lastSize.get() + ")");
			if (size - lastSize.get() > 3) {
				System.out.println("scaling up: " + topic);
				this.poolManager.addStreamApp(topic);
				lastSize.set(size);
			}
			else if (size < lastSize.get()) {
				lastSize.set(size);
			}
		}
		return null;
	}

	private static class Window {

		private final long duration;

		private final LinkedBlockingQueue<Long> queue = new LinkedBlockingQueue<>();

		private Window() {
			this.duration = 10_000;
		}

		int increment() {
			Long now = System.currentTimeMillis();
			Long next = queue.peek();
			while (next != null && now - duration >= next) {
				queue.remove();
				next = queue.peek();
			}
			queue.add(now);
			return queue.size();
		}
	}
}
