package io.pivotal.example.order.phase2;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order")
public class OrderProcessorPhase2ConfigurationProperties {

	private File directory = new File("/tmp/orders");

	public File getDirectory() {
		if (!this.directory.exists()) {
			this.directory.mkdirs();
		}
		else if (!this.directory.isDirectory()) {
			throw new IllegalArgumentException(String.format("not a directory: %s", this.directory));
		}
		return directory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}
}