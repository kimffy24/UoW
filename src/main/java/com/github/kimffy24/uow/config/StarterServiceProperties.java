package com.github.kimffy24.uow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("com.github.kimffy24.uow")
public class StarterServiceProperties {
	private String config;

	public void setConfig(String config) {
		this.config = config;
	}

	public String getConfig() {
		return config;
	}
}
