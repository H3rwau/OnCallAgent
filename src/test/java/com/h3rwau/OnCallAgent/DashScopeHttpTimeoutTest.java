package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.config.DashScopeConfig;
import com.sun.net.httpserver.HttpServer;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashScopeHttpTimeoutTest {
    private HttpServer server;
    private ExecutorService executor;
    private final CountDownLatch release = new CountDownLatch(1);
    private DashScopeConfig clients;
    private String url;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/slow", exchange -> {
            try {
                release.await(5, TimeUnit.SECONDS);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.createContext("/stream", exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().flush();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.createContext("/active-stream", exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                for (int i = 0; i < 6; i++) {
                    exchange.getResponseBody().write(("data: token" + i + "\n\n").getBytes());
                    exchange.getResponseBody().flush();
                    Thread.sleep(90);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        release.countDown();
        server.stop(0);
        executor.shutdownNow();
        if (clients != null) clients.close();
    }

    @Test
    void synchronousRequestsHonorTheConfiguredTimeout() {
        clients = new DashScopeConfig(150);
        assertThatThrownBy(() -> clients.restClientBuilder().build().get().uri(url + "/slow")
                .retrieve().body(String.class)).isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void streamingRequestsTimeOutWhenNoContentArrives() {
        clients = new DashScopeConfig(150);
        assertThatThrownBy(() -> clients.webClientBuilder().build().get().uri(url + "/stream")
                .retrieve().bodyToFlux(String.class).blockFirst(Duration.ofSeconds(3)))
                .hasRootCauseInstanceOf(ReadTimeoutException.class);
    }

    @Test
    void anActiveStreamMayLastLongerThanTheReadIdleTimeout() {
        clients = new DashScopeConfig(350);
        var tokens = clients.webClientBuilder().build().get().uri(url + "/active-stream")
                .retrieve().bodyToFlux(String.class).collectList().block(Duration.ofSeconds(3));
        assertThat(tokens).containsExactly("token0", "token1", "token2", "token3", "token4", "token5");
    }
}
