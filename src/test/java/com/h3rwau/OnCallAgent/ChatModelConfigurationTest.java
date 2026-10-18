package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.agent.tool.DateTimeTools;
import com.h3rwau.OnCallAgent.agent.tool.InternalDocsTools;
import com.h3rwau.OnCallAgent.agent.tool.QueryMetricsTools;
import com.h3rwau.OnCallAgent.config.DashScopeConfig;
import com.h3rwau.OnCallAgent.logs.LogToolCatalog;
import com.h3rwau.OnCallAgent.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatModelConfigurationTest {
    private ApplicationContextRunner runner(Map<String, Object> environment) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().replace(
                        StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                        new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                environment)))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
                .withPropertyValues("spring.ai.dashscope.api-key=test-api-key")
                .withUserConfiguration(ChatService.class, DashScopeConfig.class)
                .withBean(InternalDocsTools.class, () -> mock(InternalDocsTools.class))
                .withBean(DateTimeTools.class)
                .withBean(QueryMetricsTools.class, () -> mock(QueryMetricsTools.class))
                .withBean(LogToolCatalog.class, () -> mock(LogToolCatalog.class));
    }

    @Test
    void defaultsToQwenPlusForChatAndAiOps() {
        runner(Map.of()).run(context -> {
            assertThat(context).hasNotFailed();
            assertModels(context.getBean(ChatService.class), "qwen-plus");
        });
    }

    @Test
    void environmentVariableSelectsTheModelForBothEntryPoints() {
        runner(Map.of("DASHSCOPE_CHAT_MODEL", "qwen-max")).run(context -> {
            assertThat(context).hasNotFailed();
            assertModels(context.getBean(ChatService.class), "qwen-max");
        });
    }

    private void assertModels(ChatService service, String expectedModel) {
        var api = service.createDashScopeApi();
        var chat = service.createStandardChatModel(api).getDefaultOptions();
        var aiOps = service.createAiOpsChatModel(api).getDefaultOptions();
        assertThat(chat.getModel()).isEqualTo(expectedModel);
        assertThat(aiOps.getModel()).isEqualTo(expectedModel);
        assertThat(chat.getTemperature()).isEqualTo(0.7);
        assertThat(chat.getMaxTokens()).isEqualTo(2000);
        assertThat(aiOps.getTemperature()).isEqualTo(0.3);
        assertThat(aiOps.getMaxTokens()).isEqualTo(8000);
    }
}
