package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.service.VectorEmbeddingService;
import com.h3rwau.OnCallAgent.service.VectorSearchService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.*;
import io.milvus.param.R;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExactKnowledgeLookupTest {
    private final MilvusServiceClient milvus = mock(MilvusServiceClient.class);
    private final VectorEmbeddingService embedding = mock(VectorEmbeddingService.class);
    private final VectorSearchService search = new VectorSearchService();

    ExactKnowledgeLookupTest() {
        ReflectionTestUtils.setField(search, "milvusClient", milvus);
        ReflectionTestUtils.setField(search, "embeddingService", embedding);
        when(embedding.generateQueryVector(any())).thenReturn(List.of(0.1F));
        when(milvus.search(any(SearchParam.class))).thenReturn(R.success(SearchResults.newBuilder()
                .setStatus(Status.newBuilder().setErrorCode(ErrorCode.Success))
                .setResults(SearchResultData.newBuilder().setNumQueries(1).setTopK(1).addTopks(1)
                        .setPrimaryFieldName("id").addAllOutputFields(List.of("id", "content", "metadata"))
                        .setIds(IDs.newBuilder().setStrId(StringArray.newBuilder().addData("chunk")))
                        .addScores(0.1F).addAllFieldsData(rows("DetailErrorStatus 20：答题卡结构错误")
                                .getData().getFieldsDataList()))
                .build()));
    }

    @Test
    void classAndCodeFindTheExactStoredChunkInsteadOfNeighbouringVectorMatches() {
        when(milvus.query(any(QueryParam.class))).thenReturn(rows(
                "OMR Java DetailErrorStatus 307：DETAILERRORSTATUS_BLANK_ERROR\n打分条存在红笔划在空白格的情况"));
        var results = search.searchSimilarDocuments("DetailErrorStatus为307是什么错误码", 3);
        assertThat(results).singleElement().satisfies(result ->
                assertThat(result.getContent()).contains("307", "红笔", "空白格"));
        verifyNoInteractions(embedding);
        verify(milvus, never()).search(any(SearchParam.class));
    }

    @Test
    void partialNumbersDoNotCountAsAnExactCodeMatch() {
        when(milvus.query(any(QueryParam.class))).thenReturn(rows("DetailErrorStatus 1307 的其他定义"));
        when(embedding.generateQueryVector(any())).thenThrow(new IllegalStateException("semantic fallback reached"));
        assertThatThrownBy(() -> search.searchSimilarDocuments("DetailErrorStatus 307", 3))
                .hasMessageContaining("semantic fallback reached");
        verify(embedding).generateQueryVector("DetailErrorStatus 307");
    }

    @Test
    void codeLookupAlsoAppliesToOtherNamespacesAndNumbers() {
        when(milvus.query(any(QueryParam.class))).thenReturn(rows("OtherErrorStatus 42：another defined error"));
        assertThat(search.searchSimilarDocuments("OtherErrorStatus 42", 3)).singleElement()
                .satisfies(result -> assertThat(result.getContent()).contains("OtherErrorStatus", "42"));
        verifyNoInteractions(embedding);
    }

    @Test
    void ordinarySemanticQuestionsKeepTheExistingVectorSearchPath() {
        when(embedding.generateQueryVector(any())).thenThrow(new IllegalStateException("semantic fallback reached"));
        assertThatThrownBy(() -> search.searchSimilarDocuments("如何排查考号定位异常", 3))
                .hasMessageContaining("semantic fallback reached");
        verify(milvus, never()).query(any(QueryParam.class));
    }

    private R<QueryResults> rows(String content) {
        return R.success(QueryResults.newBuilder()
                .setStatus(Status.newBuilder().setErrorCode(ErrorCode.Success))
                .addAllOutputFields(List.of("id", "content", "metadata"))
                .addFieldsData(FieldData.newBuilder().setFieldName("id").setType(DataType.VarChar)
                        .setScalars(ScalarField.newBuilder().setStringData(StringArray.newBuilder().addData("chunk"))))
                .addFieldsData(FieldData.newBuilder().setFieldName("content").setType(DataType.VarChar)
                        .setScalars(ScalarField.newBuilder().setStringData(StringArray.newBuilder().addData(content))))
                .addFieldsData(FieldData.newBuilder().setFieldName("metadata").setType(DataType.VarChar)
                        .setScalars(ScalarField.newBuilder().setStringData(StringArray.newBuilder().addData("{}"))))
                .build());
    }
}
