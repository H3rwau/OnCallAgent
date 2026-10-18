package com.h3rwau.OnCallAgent.logs;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** The single entry point used by normal chat and every AI Ops Agent. */
@Component
public class LogToolCatalog {
    private final LogIntegrationProperties properties;
    private final LogSourceCatalog sources;
    private final ObjectProvider<ToolCallbackProvider> providers;

    public LogToolCatalog(LogIntegrationProperties properties, LogSourceCatalog sources,
                          ObjectProvider<ToolCallbackProvider> providers) {
        this.properties = properties;
        this.sources = sources;
        this.providers = providers;
    }

    public ToolCallback[] toolCallbacks() {
        var enabled = properties.enabledServers();
        if (enabled.isEmpty()) return new ToolCallback[0];
        Map<String, ToolCallback> tools = new LinkedHashMap<>();
        providers.orderedStream().forEach(provider -> {
            for (ToolCallback callback : provider.getToolCallbacks()) {
                String name = callback.getToolDefinition().name();
                if (enabled.keySet().stream().noneMatch(id -> name.startsWith(id + "__"))) continue;
                if (tools.putIfAbsent(name, callback) != null) {
                    throw new IllegalStateException("发现重复的 MCP 日志工具名称: " + name);
                }
            }
        });
        return tools.values().toArray(ToolCallback[]::new);
    }

    public String context() {
        if (properties.isMockEnabled()) {
            return "\n当前启用本地模拟日志模式；只使用本地模拟日志工具，所有模拟结果必须明确标记为模拟数据。\n";
        }
        StringBuilder context = new StringBuilder("""
                \n日志查询规则：
                1. 根据日志源名称或别名确定所属服务，只调用该服务的实际可用工具，不混用不同服务的资源和参数。
                2. 工具参数与查询语言以实际工具定义和服务说明为准；日志源目录提供默认资源，用户明确指定的信息优先。
                3. 信息不足时，使用已发现的资源列举工具确认或说明缺失内容；禁止编造日志源、地域和查询结果。
                4. 未指定时间时优先查询最近一小时，原始日志最多返回20条；参数名称和时间单位遵循工具定义。
                5. 查询失败必须反馈真实原因；配置中的默认资源不替代云端访问权限，也不会转换各服务的查询语法。
                """);
        var enabled = properties.enabledServers();
        ToolCallback[] tools = toolCallbacks();
        if (enabled.isEmpty()) return context.append("当前未启用真实日志服务，无法查询真实云日志。\n").toString();
        enabled.forEach((id, server) -> {
            context.append("\n日志服务 ID=").append(id).append("，名称=")
                    .append(server.getDisplayName().isBlank() ? id : server.getDisplayName()).append('\n');
            if (!server.getInstructions().isBlank()) context.append("服务说明：").append(server.getInstructions()).append('\n');
            context.append("可用工具：\n");
            for (ToolCallback callback : tools) {
                if (callback.getToolDefinition().name().startsWith(id + "__")) {
                    context.append("- ").append(callback.getToolDefinition().name()).append('\n');
                }
            }
        });
        return context.append(sources.context()).toString();
    }
}
