package com.h3rwau.OnCallAgent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.h3rwau.OnCallAgent.agent.tool.DateTimeTools;
import com.h3rwau.OnCallAgent.agent.tool.InternalDocsTools;
import com.h3rwau.OnCallAgent.agent.tool.QueryMetricsTools;
import com.h3rwau.OnCallAgent.logs.LogToolCatalog;
import com.h3rwau.OnCallAgent.service.ChatService;
import com.h3rwau.OnCallAgent.service.VectorSearchService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatRetrievalContextTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void agentPreservesTheCurrentCodeDespiteStaleHistoryAndABroadenedToolQuery(boolean streaming) throws Exception {
        VectorSearchService search = mock(VectorSearchService.class);
        var chunk = new VectorSearchService.SearchResult();
        chunk.setContent("DetailErrorStatus 307 DETAILERRORSTATUS_BLANK_ERROR：打分条存在红笔划在空白格的情况");
        when(search.searchSimilarDocuments(anyString(), anyInt())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("307") ? List.of(chunk) : List.of());
        ChatService service = new ChatService();
        ReflectionTestUtils.setField(service, "internalDocsTools", new InternalDocsTools(search));
        ReflectionTestUtils.setField(service, "dateTimeTools", new DateTimeTools());
        ReflectionTestUtils.setField(service, "queryMetricsTools", mock(QueryMetricsTools.class));
        LogToolCatalog catalog = mock(LogToolCatalog.class);
        when(catalog.context()).thenReturn("");
        when(catalog.toolCallbacks()).thenReturn(new ToolCallback[0]);
        ReflectionTestUtils.setField(service, "logToolCatalog", catalog);

        DashScopeChatModel model = mock(DashScopeChatModel.class);
        when(model.getDefaultOptions()).thenReturn(DashScopeChatOptions.builder().withModel("test-model").build());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> reply(invocation.getArgument(0)));
        when(model.stream(any(Prompt.class))).thenAnswer(invocation -> Flux.just(reply(invocation.getArgument(0))));
        String prompt = service.buildSystemPrompt(List.of(
                Map.of("role", "user", "content", "DetailErrorStatus 307是什么？"),
                Map.of("role", "assistant", "content", "之前未检索到307，内部文档不可用。")));
        var agent = service.createReactAgent(model, prompt, "DetailErrorStatus为307是什么错误码");
        if (streaming) {
            agent.stream("DetailErrorStatus为307是什么错误码").blockLast();
        } else {
            assertThat(agent.call("DetailErrorStatus为307是什么错误码").getText()).contains("红笔", "空白格");
        }
        verify(search).searchSimilarDocuments(argThat(query -> query.contains("DetailErrorStatus")
                && query.contains("307")), anyInt());
    }

    private ChatResponse reply(Prompt prompt) {
        String evidence = prompt.getInstructions().stream()
                .filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast)
                .flatMap(message -> message.getResponses().stream())
                .map(ToolResponseMessage.ToolResponse::responseData).findFirst().orElse(null);
        AssistantMessage message = evidence == null
                ? AssistantMessage.builder().content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                        "lookup", "function", "queryInternalDocs", "{\"query\":\"DetailErrorStatus OMR\"}"))).build()
                : new AssistantMessage(evidence);
        return new ChatResponse(List.of(new Generation(message)));
    }
}
