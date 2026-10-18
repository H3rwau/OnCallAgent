package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.logs.LogMcpConnectionConfiguration;
import com.h3rwau.OnCallAgent.logs.LogSourceCatalog;
import com.h3rwau.OnCallAgent.logs.LogToolCatalog;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.webflux.autoconfigure.SseWebFluxTransportAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.annotations.McpClientAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpSseClientConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Opt-in: connects to an already running SSE server and reads at most one row per logstore. */
@EnabledIfEnvironmentVariable(named = "SLS_MCP_INTEGRATION_TEST", matches = "true")
class AliyunSlsMcpIntegrationTest {

    @Test
    void springDiscoversSlsToolsAndReadsConfiguredLogstores() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(LogMcpConnectionConfiguration.class, LogSourceCatalog.class, LogToolCatalog.class)
                .withConfiguration(AutoConfigurations.of(
                        JacksonAutoConfiguration.class,
                        SseWebFluxTransportAutoConfiguration.class,
                        McpClientAnnotationScannerAutoConfiguration.class,
                        McpClientAutoConfiguration.class,
                        McpToolCallbackAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var catalog = context.getBean(LogToolCatalog.class);
                    var callbacks = catalog.toolCallbacks();
                    var names = Arrays.stream(callbacks).map(c -> c.getToolDefinition().name()).toList();
                    assertThat(names).hasSize(3);
                    for (String tool : List.of("sls_list_projects", "sls_list_logstores", "sls_execute_sql")) {
                        assertThat(names.stream().anyMatch(name -> name.endsWith(tool))).isTrue();
                    }
                    assertThat(context.getBean(McpSseClientConnectionDetails.class).getConnections())
                            .containsOnlyKeys("aliyun-sls");
                    assertThat(catalog.context()).contains("gb-orchestration", "cn-beijing", "aliyun-sls__sls_execute_sql");
                    assertThat(context.getEnvironment().getProperty("spring.ai.mcp.client.stdio.connections.aliyun-sls.command"))
                            .isNull();
                    assertThat(context.getEnvironment().getProperty("spring.ai.mcp.client.sse.connections.tencent-cls.url"))
                            .isNull();
                    @SuppressWarnings("unchecked")
                    var clients = (List<McpSyncClient>) context.getBean("mcpSyncClients");
                    assertThat(clients).hasSize(1);
                    var client = clients.getFirst();
                    for (String store : List.of("aicorr-gb-corr-log", "aicorr-gb-thor-log", "aicorr-gb-luke-biz-log")) {
                        var result = client.callTool(new McpSchema.CallToolRequest("sls_execute_sql", Map.of(
                                "regionId", "cn-beijing", "project", "aicorr-gb", "logStore", store,
                                "query", "*", "from_time", "now-5m", "to_time", "now", "limit", 1)));
                        assertFalse(Boolean.TRUE.equals(result.isError()), "SLS query failed for " + store);
                        assertThat(result.content()).isNotEmpty();
                        System.out.println("SLS read succeeded: " + store);
                    }
                });
    }
}
