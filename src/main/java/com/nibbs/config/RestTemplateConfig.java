package com.nibbs.config;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	@Value("${truststore.location}")
	private Resource trustStore;

	@Value("${truststore.password}")
	private String trustStorePassword;

	@Bean(name = "restTemplate")
	@ConditionalOnProperty(name="custom.trust", havingValue="true")
	public RestTemplate restTemplateWithTrustStore(RestTemplateBuilder builder) throws IOException,
			CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(trustStore.getURL(), trustStorePassword.toCharArray()).build();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

		return builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
				.setConnectTimeout(Duration.ofMillis(30000)).setReadTimeout(Duration.ofMillis(300000)).build();
	}
	
	@Bean(name = "restTemplate")
	public RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplateBuilder()
				.setConnectTimeout(Duration.ofMillis(30000))
				.setReadTimeout(Duration.ofMillis(300000))
				.build();
		return restTemplate;
	}

}
