package com.h3rwau.OnCallAgent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.h3rwau.OnCallAgent.agent.tool.DateTimeTools;
import com.h3rwau.OnCallAgent.agent.tool.InternalDocsTools;
import com.h3rwau.OnCallAgent.agent.tool.QueryLogsTools;
import com.h3rwau.OnCallAgent.agent.tool.QueryMetricsTools;
import com.h3rwau.OnCallAgent.service.VectorSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exercises the schema generation performed by ReactAgent.builder().methodTools(...). */
class ToolSchemaCompatibilityTest {
    @Test
    void generatesSchemasForTheActualAgentMethodTools() throws Exception {
        var callbacks = ToolCallbacks.from(new DateTimeTools(),
                new InternalDocsTools(mock(VectorSearchService.class)), new QueryMetricsTools(), new QueryLogsTools());
        assertThat(Arrays.stream(callbacks).map(callback -> callback.getToolDefinition().name()))
                .contains("getCurrentDateTime", "queryInternalDocs", "queryPrometheusAlerts", "queryLogs", "getAvailableLogTopics");
        var mapper = new ObjectMapper();
        for (var callback : callbacks) {
            var schema = mapper.readTree(callback.getToolDefinition().inputSchema());
            assertThat(schema.path("type").asText()).isEqualTo("object");
        }
    }
}
