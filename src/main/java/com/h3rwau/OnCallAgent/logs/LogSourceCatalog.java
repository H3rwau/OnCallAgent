package com.h3rwau.OnCallAgent.logs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Business names and resource defaults stay in configuration rather than Agent prompts. */
@Component
public class LogSourceCatalog {
    private final LogIntegrationProperties properties;
    private final ObjectMapper objectMapper;

    public LogSourceCatalog(LogIntegrationProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Map<String, LogIntegrationProperties.Source> availableSources() {
        Map<String, LogIntegrationProperties.Source> available = new LinkedHashMap<>();
        var servers = properties.enabledServers();
        properties.getSources().forEach((id, source) -> {
            if (source.isEnabled() && servers.containsKey(source.getServer())) available.put(id, source);
        });
        return Collections.unmodifiableMap(available);
    }

    public String context() {
        StringBuilder context = new StringBuilder("\n日志源目录（资源默认值，不代表权限授权）：\n");
        availableSources().forEach((id, source) -> {
            context.append("- ID=").append(id)
                    .append("，名称=").append(source.getName())
                    .append("，所属服务=").append(source.getServer());
            if (id.equals(properties.getDefaultSource())) context.append("（默认日志源）");
            context.append("，别名=").append(json(source.getAliases()))
                    .append("，默认参数=").append(json(source.getDefaults())).append('\n');
        });
        if (availableSources().isEmpty()) context.append("未配置可用日志源；需要用户提供资源信息或用实际发现工具确认。\n");
        return context.toString();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("日志源配置无法序列化", error);
        }
    }
}
