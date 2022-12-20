package com.nibss.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
	@ConditionalOnProperty(name="ignore.ssl", havingValue="true")
	public RestTemplate restTemplateIgnoreSsl(RestTemplateBuilder builder) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
		SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);
		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		return builder.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
				.setConnectTimeout(Duration.ofMillis(30000)).setReadTimeout(Duration.ofMillis(300000)).build();
	}
	
	@Bean(name = "restTemplate")
	public RestTemplate getRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		InetSocketAddress address = new InetSocketAddress("172.23.12.67", Integer.parseInt("4145"));
		Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
		factory.setProxy(proxy);
		RestTemplate restTemplate = new RestTemplateBuilder()
				.setConnectTimeout(Duration.ofMillis(30000))
				.setReadTimeout(Duration.ofMillis(300000))
				.build();
		restTemplate.setRequestFactory(factory);
		return restTemplate;
	}

}
