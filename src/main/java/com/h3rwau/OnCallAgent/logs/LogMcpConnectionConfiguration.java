package com.h3rwau.OnCallAgent.logs;

import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.ai.mcp.client.common.autoconfigure.McpSseClientConnectionDetails;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpSseClientProperties.SseParameters;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Supplies enabled connections; Spring AI still owns creation and closure of MCP clients. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LogIntegrationProperties.class)
public class LogMcpConnectionConfiguration {

    @Bean
    @Primary
    public McpSseClientConnectionDetails logMcpConnectionDetails(LogIntegrationProperties properties) {
        return () -> {
            Map<String, SseParameters> connections = new LinkedHashMap<>();
            properties.enabledServers().forEach((id, server) ->
                    connections.put(id, new SseParameters(server.getUrl(), server.getSseEndpoint())));
            return connections;
        };
    }

    @Bean
    public McpToolNamePrefixGenerator logToolNamePrefixGenerator() {
        // Spring AI puts the named SSE connection ID into the sync client's title.
        return (connection, tool) -> toolName(connection.clientInfo().title(), tool.name());
    }

    static String toolName(String serverId, String nativeName) {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("MCP 日志工具缺少所属连接 ID");
        }
        String original = serverId + "__" + nativeName;
        if (original.matches("[a-zA-Z0-9_-]{1,64}")) return original;
        String safe = original.replaceAll("[^a-zA-Z0-9_-]", "_");
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(original.getBytes(StandardCharsets.UTF_8))).substring(0, 8);
            return safe.substring(0, Math.min(55, safe.length())) + "_" + hash;
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
