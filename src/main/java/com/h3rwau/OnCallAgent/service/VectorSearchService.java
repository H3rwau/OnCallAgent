package com.h3rwau.OnCallAgent.service;

import com.h3rwau.OnCallAgent.constant.MilvusConstants;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.R;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.param.dml.QueryParam;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;

/**
 * 向量搜索服务
 * 负责从 Milvus 中搜索相似向量
 */
@Service
public class VectorSearchService {

    private static final Logger logger = LoggerFactory.getLogger(VectorSearchService.class);

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private VectorEmbeddingService embeddingService;

    /**
     * 搜索相似文档
     *
     * @param query 查询文本
     * @param topK 返回最相似的K个结果
     * @return 搜索结果列表
     */
    public List<SearchResult> searchSimilarDocuments(String query, int topK) {
        try {
            logger.info("开始搜索相似文档, 查询: {}, topK: {}", query, topK);
            if (topK < 1) {
                throw new IllegalArgumentException("topK must be positive");
            }
            List<SearchResult> exact = searchExactTechnicalCode(query, topK);
            if (!exact.isEmpty()) {
                logger.info("精确标识检索完成, 找到 {} 个文档片段", exact.size());
                return exact;
            }

            // 1. 将查询文本向量化
            List<Float> queryVector = embeddingService.generateQueryVector(query);
            logger.debug("查询向量生成成功, 维度: {}", queryVector.size());

            // 2. 构建搜索参数
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                    .withVectorFieldName("vector")
                    .withVectors(Collections.singletonList(queryVector))
                    .withTopK(topK)
                    .withMetricType(io.milvus.param.MetricType.L2)
                    .withOutFields(List.of("id", "content", "metadata"))
                    .withParams("{\"nprobe\":10}")
                    .build();

            // 3. 执行搜索
            R<SearchResults> searchResponse = milvusClient.search(searchParam);

            if (searchResponse.getStatus() != 0) {
                throw new RuntimeException("向量搜索失败: " + searchResponse.getMessage());
            }

            // 4. 解析搜索结果
            SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
            List<SearchResult> results = new ArrayList<>();

            for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
                SearchResult result = new SearchResult();
                result.setId((String) wrapper.getIDScore(0).get(i).get("id"));
                result.setContent((String) wrapper.getFieldData("content", 0).get(i));
                result.setScore(wrapper.getIDScore(0).get(i).getScore());
                result.setRetrievalMethod("semantic");

                // 解析 metadata
                Object metadataObj = wrapper.getFieldData("metadata", 0).get(i);
                if (metadataObj != null) {
                    result.setMetadata(metadataObj.toString());
                }

                results.add(result);
            }

            logger.info("搜索完成, 找到 {} 个相似文档", results.size());
            return results;

        } catch (Exception e) {
            logger.error("搜索相似文档失败", e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    /** Exact code lookup uses the stored chunks, without a schema change or re-index. */
    private List<SearchResult> searchExactTechnicalCode(String query, int topK) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        var names = Pattern.compile("[A-Za-z_][A-Za-z0-9_.]*").matcher(query);
        while (names.find() && identifiers.size() < 3) {
            String name = names.group();
            if ((name.matches(".*[A-Z].*") && name.matches(".*[a-z].*"))
                    || name.contains("_") || name.contains(".")) {
                identifiers.add(name);
            }
        }
        LinkedHashSet<String> numbers = new LinkedHashSet<>();
        var codes = Pattern.compile("(?<![A-Za-z0-9_])\\d+(?![A-Za-z0-9_])").matcher(query);
        while (codes.find() && numbers.size() < 3) {
            numbers.add(codes.group());
        }
        if (identifiers.isEmpty() || numbers.isEmpty()) {
            return List.of();
        }
        List<String> terms = new ArrayList<>(identifiers);
        terms.addAll(numbers);
        // Terms consist solely of identifier characters/numbers, so cannot inject a filter.
        String expression = terms.stream().map(term -> "content like \"%" + term + "%\"")
                .collect(java.util.stream.Collectors.joining(" && "));
        var response = milvusClient.query(QueryParam.newBuilder()
                .withCollectionName(MilvusConstants.MILVUS_COLLECTION_NAME)
                .withExpr(expression)
                .withOutFields(List.of("id", "content", "metadata"))
                .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withLimit(Math.min(256L, Math.max(32L, (long) topK * 4)))
                .build());
        if (response.getStatus() != 0) {
            throw new IllegalStateException("精确标识检索失败: " + response.getMessage());
        }
        List<SearchResult> results = new ArrayList<>();
        for (var row : new QueryResultsWrapper(response.getData()).getRowRecords()) {
            String content = (String) row.get("content");
            boolean matches = terms.stream().allMatch(term -> Pattern.compile(
                    "(?<![A-Za-z0-9_])" + Pattern.quote(term) + "(?![A-Za-z0-9_])")
                    .matcher(content).find());
            if (!matches) {
                continue;
            }
            SearchResult result = new SearchResult();
            result.setId((String) row.get("id"));
            result.setContent(content);
            result.setRetrievalMethod("exact");
            Object metadata = row.get("metadata");
            result.setMetadata(metadata == null ? null : metadata.toString());
            results.add(result);
            if (results.size() >= topK) {
                break;
            }
        }
        return results;
    }

    /**
     * 搜索结果类
     */
    @Setter
    @Getter
    public static class SearchResult {
        private String id;
        private String content;
        private Float score;
        private String retrievalMethod;
        private String metadata;

    }
}
