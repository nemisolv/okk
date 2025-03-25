package com.vht.springbootdemo.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Quản lý Pooling cho HttpClient
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100); // Tổng số kết nối tối đa
        connectionManager.setDefaultMaxPerRoute(20); // Số kết nối tối đa mỗi route

        // Cấu hình request timeout
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5)) // Timeout khi kết nối
                .setResponseTimeout(Timeout.ofSeconds(10)) // Timeout khi nhận phản hồi
                .setConnectionRequestTimeout(Timeout.ofSeconds(5)) // Timeout khi lấy kết nối từ pool
                .build();

        // Khởi tạo HttpClient với cấu hình trên
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections() // Xóa kết nối hết hạn
                .evictIdleConnections(Timeout.ofSeconds(30)) // Dọn dẹp kết nối idle sau 30s
                .build();

        // Tạo factory để dùng HttpClient với RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
