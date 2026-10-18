package com.h3rwau.OnCallAgent.logs;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cloud-neutral configuration; server IDs are configuration keys, not Java enums. */
@Data
@ConfigurationProperties(prefix = "oncall.logs")
public class LogIntegrationProperties implements InitializingBean {
    private boolean mockEnabled;
    private String defaultSource = "";
    private Map<String, Server> servers = new LinkedHashMap<>();
    private Map<String, Source> sources = new LinkedHashMap<>();

    @Data
    public static class Server {
        private boolean enabled;
        private String displayName = "";
        private String url = "";
        private String sseEndpoint = "/sse";
        private String instructions = "";
    }

    @Data
    public static class Source {
        private boolean enabled = true;
        private String server = "";
        private String name = "";
        private List<String> aliases = List.of();
        private Map<String, Object> defaults = new LinkedHashMap<>();
    }

    public Map<String, Server> enabledServers() {
        Map<String, Server> enabled = new LinkedHashMap<>();
        if (!mockEnabled) servers.forEach((id, server) -> {
            if (server.isEnabled()) enabled.put(id, server);
        });
        return Collections.unmodifiableMap(enabled);
    }

    @Override
    public void afterPropertiesSet() {
        servers.forEach((id, server) -> {
            require(id.matches("[a-z][a-z0-9-]{0,31}"), "日志服务 ID 需为小写字母、数字、连字符且不超过32字符: " + id);
            if (!server.isEnabled() || mockEnabled) return;
            require(StringUtils.hasText(server.getUrl()), "启用的日志服务缺少 url: " + id);
            try {
                URI url = URI.create(server.getUrl());
                require(("http".equals(url.getScheme()) || "https".equals(url.getScheme())) && url.getHost() != null
                        && url.getUserInfo() == null && url.getQuery() == null && url.getFragment() == null,
                        "日志服务 url 必须是 HTTP(S) 基础地址，不能包含认证信息、查询参数或片段: " + id);
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("日志服务 url 配置不正确: " + id);
            }
            require(StringUtils.hasText(server.getSseEndpoint()) && server.getSseEndpoint().startsWith("/"),
                    "日志服务 sse-endpoint 必须以 / 开头: " + id);
        });
        sources.forEach((id, source) -> {
            if (!source.isEnabled()) return;
            require(servers.containsKey(source.getServer()), "日志源引用了不存在的服务: " + id);
            require(StringUtils.hasText(source.getName()), "日志源缺少 name: " + id);
        });
        if (!mockEnabled && StringUtils.hasText(defaultSource)) {
            Source source = sources.get(defaultSource);
            require(source != null && source.isEnabled() && enabledServers().containsKey(source.getServer()),
                    "default-source 必须引用已启用服务下的有效日志源: " + defaultSource);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
