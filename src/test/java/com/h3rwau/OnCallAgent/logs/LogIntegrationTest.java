package com.h3rwau.OnCallAgent.logs;

import com.h3rwau.OnCallAgent.agent.tool.QueryLogsTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.client.common.autoconfigure.McpClientAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.McpSseClientConnectionDetails;
import org.springframework.ai.mcp.client.common.autoconfigure.McpToolCallbackAutoConfiguration;
import org.springframework.ai.mcp.client.common.autoconfigure.annotations.McpClientAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.client.webflux.autoconfigure.SseWebFluxTransportAutoConfiguration;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogIntegrationTest {
    private final ApplicationContextRunner catalogRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
            .withUserConfiguration(LogMcpConnectionConfiguration.class, LogSourceCatalog.class, LogToolCatalog.class);

    @Test
    void onlyEnabledServersBecomeSseConnectionsIncludingAnUnknownVendor() {
        catalogRunner.withPropertyValues(
                "oncall.logs.servers.cloud-one.enabled=true",
                "oncall.logs.servers.cloud-one.url=http://127.0.0.1:8181",
                "oncall.logs.servers.cloud-one.sse-endpoint=/events",
                "oncall.logs.servers.future-cloud.enabled=true",
                "oncall.logs.servers.future-cloud.url=http://127.0.0.1:8183",
                "oncall.logs.servers.disabled-cloud.enabled=false",
                "oncall.logs.servers.disabled-cloud.url=not-a-url"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            var connections = context.getBean(McpSseClientConnectionDetails.class).getConnections();
            assertThat(connections).containsOnlyKeys("cloud-one", "future-cloud");
            assertThat(connections.get("cloud-one").sseEndpoint()).isEqualTo("/events");
        });
    }

    @Test
    void configurationGeneratesToolsAndResourceContextWithoutVendorCode() {
        ToolCallback active = callback("future-cloud__search_logs");
        ToolCallback disabled = callback("disabled-cloud__search_logs");
        catalogRunner.withBean("fixtureTools", ToolCallbackProvider.class, () -> () -> new ToolCallback[]{active, disabled})
                .withPropertyValues(
                        "oncall.logs.servers.future-cloud.enabled=true",
                        "oncall.logs.servers.future-cloud.url=http://127.0.0.1:8183",
                        "oncall.logs.servers.future-cloud.display-name=Future Logs",
                        "oncall.logs.servers.future-cloud.instructions=Use streamKey from the native schema.",
                        "oncall.logs.servers.disabled-cloud.enabled=false",
                        "oncall.logs.default-source=orders",
                        "oncall.logs.sources.orders.server=future-cloud",
                        "oncall.logs.sources.orders.name=订单服务",
                        "oncall.logs.sources.orders.aliases[0]=订单日志",
                        "oncall.logs.sources.orders.defaults[streamKey]=stream-42",
                        "oncall.logs.sources.hidden.server=disabled-cloud",
                        "oncall.logs.sources.hidden.name=隐藏日志"
                ).run(context -> {
                    assertThat(context).hasNotFailed();
                    var catalog = context.getBean(LogToolCatalog.class);
                    assertThat(catalog.toolCallbacks()).containsExactly(active);
                    assertThat(catalog.context()).contains("Future Logs", "订单服务", "订单日志", "stream-42",
                            "future-cloud__search_logs", "默认日志源", "Use streamKey");
                    assertThat(catalog.context()).doesNotContain("隐藏日志", "disabled-cloud__search_logs");
                });
    }

    @Test
    void mockModeSkipsRealConnectionsAndRegistersOnlyTheLocalMock() {
        catalogRunner.withConfiguration(AutoConfigurations.of(SseWebFluxTransportAutoConfiguration.class,
                        McpClientAnnotationScannerAutoConfiguration.class, McpClientAutoConfiguration.class,
                        McpToolCallbackAutoConfiguration.class))
                .withUserConfiguration(QueryLogsTools.class)
                .withPropertyValues("oncall.logs.mock-enabled=true",
                        "oncall.logs.servers.offline.enabled=true",
                        "oncall.logs.servers.offline.url=http://127.0.0.1:9")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(QueryLogsTools.class);
                    assertThat(context.getBean(McpSseClientConnectionDetails.class).getConnections()).isEmpty();
                    assertThat(context.getBean(LogToolCatalog.class).toolCallbacks()).isEmpty();
                    assertThat(context.getBean(LogToolCatalog.class).context()).contains("模拟日志模式");
                    assertThat(context.getBean(QueryLogsTools.class).queryLogs(null, "application-logs", "", 1))
                            .contains("模拟数据");
                });
    }

    @Test
    void disabledServersDoNotRequireCredentialsOrContactOfflineEndpoints() {
        catalogRunner.withConfiguration(AutoConfigurations.of(SseWebFluxTransportAutoConfiguration.class,
                        McpClientAnnotationScannerAutoConfiguration.class, McpClientAutoConfiguration.class,
                        McpToolCallbackAutoConfiguration.class))
                .withUserConfiguration(QueryLogsTools.class)
                .withPropertyValues("oncall.logs.servers.offline.enabled=false",
                        "oncall.logs.servers.offline.url=http://127.0.0.1:9")
                .run(context -> {
                    assertThat(context).hasNotFailed().doesNotHaveBean(QueryLogsTools.class);
                    assertThat(context.getBean(McpSseClientConnectionDetails.class).getConnections()).isEmpty();
                    assertThat(context.getBean(LogToolCatalog.class).toolCallbacks()).isEmpty();
                    assertThat(context.getBean(LogToolCatalog.class).context()).contains("未启用真实日志服务");
                });
    }

    @Test
    void invalidEnabledConnectionsAndResourceReferencesFailClearly() {
        catalogRunner.withPropertyValues("oncall.logs.servers.broken.enabled=true")
                .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining("缺少 url: broken"));
        catalogRunner.withPropertyValues("oncall.logs.sources.orders.server=missing", "oncall.logs.sources.orders.name=订单")
                .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining("不存在的服务: orders"));
        catalogRunner.withPropertyValues("oncall.logs.default-source=missing")
                .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining("default-source"));
        catalogRunner.withPropertyValues("oncall.logs.servers.broken.enabled=true", "oncall.logs.servers.broken.url=file:///tmp/logs")
                .run(context -> assertThat(context.getStartupFailure()).hasStackTraceContaining("url 配置不正确: broken"));
    }

    @Test
    void allCloudConfigurationLivesInTheMainYamlAndPreservesNativeArgumentNames() {
        catalogRunner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("oncall.logs.mock-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(LogIntegrationProperties.class);
                    assertThat(properties.getServers()).containsKeys("aliyun-sls", "tencent-cls", "huawei-lts");
                    assertThat(properties.getSources().get("gb-orchestration").getDefaults())
                            .containsEntry("regionId", "cn-beijing")
                            .containsEntry("logStore", "aicorr-gb-thor-log");
                });
    }

    @Test
    void namespaceNamesAreDistinctStableAndWithinModelLimits() {
        assertThat(LogMcpConnectionConfiguration.toolName("cloud-one", "query_logs"))
                .isEqualTo("cloud-one__query_logs")
                .isNotEqualTo(LogMcpConnectionConfiguration.toolName("cloud-two", "query_logs"));
        String longName = LogMcpConnectionConfiguration.toolName("future-cloud", "query_".repeat(30));
        assertThat(longName).hasSize(64).startsWith("future-cloud__");
        assertThat(LogMcpConnectionConfiguration.toolName("future-cloud", "query/a"))
                .isNotEqualTo(LogMcpConnectionConfiguration.toolName("future-cloud", "query_a"));
    }

    private static ToolCallback callback(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(ToolDefinition.builder()
                .name(name).description("Test log search").inputSchema("{\"type\":\"object\"}").build());
        return callback;
    }
}
