package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.agent.tool.InternalDocsTools;
import com.h3rwau.OnCallAgent.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InternalDocsLookupTest {
    @Test
    void rewrittenToolQueryRetainsTheOriginalErrorCodeThroughToolContext() {
        VectorSearchService search = mock(VectorSearchService.class);
        when(search.searchSimilarDocuments(anyString(), anyInt())).thenReturn(List.of());
        var tool = ToolCallbacks.from(new InternalDocsTools(search))[0];
        tool.call("{\"query\":\"DetailErrorStatus OMR\"}", new ToolContext(
                Map.of("oncall.originalQuestion", "DetailErrorStatus为307是什么错误码")));
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(search).searchSimilarDocuments(query.capture(), anyInt());
        assertThat(query.getValue()).contains("DetailErrorStatus", "307");
        assertThat(tool.getToolDefinition().inputSchema()).doesNotContain("toolContext");
    }
}
