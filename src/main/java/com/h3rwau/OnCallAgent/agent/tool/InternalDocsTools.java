package com.h3rwau.OnCallAgent.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h3rwau.OnCallAgent.service.VectorSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内部文档查询工具
 * 使用 RAG (Retrieval-Augmented Generation) 从内部知识库检索相关文档
 */
@Component
public class InternalDocsTools {

    private static final Logger logger = LoggerFactory.getLogger(InternalDocsTools.class);

    /** 工具名常量，用于动态构建提示词 */
    public static final String TOOL_QUERY_INTERNAL_DOCS = "queryInternalDocs";
    public static final String ORIGINAL_QUESTION_CONTEXT = "oncall.originalQuestion";

    private final VectorSearchService vectorSearchService;

    @Value("${rag.top-k:3}")
    private int topK = 3; // 默认值

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数注入依赖
     * Spring 会自动注入 VectorSearchService
     */
    @Autowired
    public InternalDocsTools(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * 查询内部文档工具
     *
     * @param query 搜索查询，描述您要查找的信息
     * @return JSON 格式的搜索结果，包含相关文档内容、相似度分数和元数据
     */
    @Tool(description = "Search the current internal knowledge base for system-specific facts, error-code definitions, " +
            "procedures and technical guides. Preserve exact class/field names and numeric error codes in the query. " +
            "The knowledge base can change after a conversation starts; search again instead of relying on past " +
            "claims that documents are missing or unavailable. A no-results response is not a service outage.")
    public String queryInternalDocs(
            @ToolParam(description = "Search query describing what information you are looking for")
            String query, ToolContext toolContext) {

        try {
            Object original = toolContext == null ? null : toolContext.getContext().get(ORIGINAL_QUESTION_CONTEXT);
            String retrievalQuery = original instanceof String question && !question.isBlank()
                    ? question + "\n检索补充：" + query : query;
            // Keep the actual user's identifiers even when the model broadens its tool query.
            List<VectorSearchService.SearchResult> searchResults =
                    vectorSearchService.searchSimilarDocuments(retrievalQuery, topK);

            if (searchResults.isEmpty()) {
                return "{\"status\": \"no_results\", \"message\": \"No relevant documents found in the knowledge base.\"}";
            }

            // 将搜索结果转换为 JSON 格式
            String resultJson = objectMapper.writeValueAsString(searchResults);


            return resultJson;

        } catch (Exception e) {
            logger.error("[工具错误] queryInternalDocs 执行失败", e);
            return String.format("{\"status\": \"error\", \"message\": \"Failed to query internal docs: %s\"}",
                    e.getMessage());
        }
    }
}
