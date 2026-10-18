package com.h3rwau.OnCallAgent.config;

import io.netty.channel.ChannelOption;
import jakarta.annotation.PreDestroy;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/** HTTP settings for manually created DashScope clients; leaves MCP's clients independent. */
@Configuration(proxyBeanMethods = false)
public class DashScopeConfig {
    private final OkHttpClient httpClient;
    private final HttpClient streamingClient;

    public DashScopeConfig(@Value("${spring.ai.dashscope.chat.options.timeout:180000}") int timeoutMillis) {
        Assert.isTrue(timeoutMillis > 0, "DashScope HTTP timeout must be positive (milliseconds)");
        Duration timeout = Duration.ofMillis(timeoutMillis);
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .writeTimeout(timeout)
                .callTimeout(timeout)
                .build();
        streamingClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
                .responseTimeout(timeout);
    }

    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().requestFactory(new OkHttp3ClientHttpRequestFactory(httpClient));
    }

    public WebClient.Builder webClientBuilder() {
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(streamingClient));
    }

    @PreDestroy
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
