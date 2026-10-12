package com.h3rwau.OnCallAgent.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置检查控制器
 * 用于验证环境变量是否正确加载
 */
@RestController
@RequestMapping("/api/config")
public class ConfigCheckController {

    @Value("${dashscope.api.key}")
    private String apiKey;

    @Value("${dashscope.embedding.model}")
    private String embeddingModel;

    @Value("${milvus.host}")
    private String milvusHost;

    @Value("${milvus.port}")
    private int milvusPort;

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * 检查配置是否正确加载
     * 访问: http://localhost:9900/api/config/check
     */
    @GetMapping("/check")
    public Map<String, Object> checkConfig() {
        Map<String, Object> config = new HashMap<>();

        // 脱敏显示 API Key（只显示前后几位）
        String maskedKey = apiKey != null && apiKey.length() > 12
            ? apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4)
            : "未配置";

        config.put("dashscope.api.key", maskedKey);
        config.put("dashscope.embedding.model", embeddingModel);
        config.put("milvus.host", milvusHost);
        config.put("milvus.port", milvusPort);
        config.put("file.upload.path", uploadPath);

        return config;
    }
}
