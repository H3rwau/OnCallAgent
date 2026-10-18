package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.agent.tool.QueryMetricsTools;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class QueryMetricsConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(QueryMetricsTools.class);

    @Test
    void mainConfigurationCanCreateTheMetricsToolWithoutExternalSettings() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(QueryMetricsTools.class);
            assertThat(context.getEnvironment().getProperty("prometheus.base-url")).isNotBlank();
            assertThat(ReflectionTestUtils.getField(context.getBean(QueryMetricsTools.class), "prometheusBaseUrl"))
                    .isEqualTo(context.getEnvironment().getProperty("prometheus.base-url"));
            assertThat(ReflectionTestUtils.getField(context.getBean(QueryMetricsTools.class), "mockEnabled"))
                    .isEqualTo(false);
        });
    }

    @Test
    void explicitMetricsSettingsOverrideTheDefaults() {
        runner.withPropertyValues("prometheus.base-url=http://127.0.0.1:19090", "prometheus.timeout=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(ReflectionTestUtils.getField(context.getBean(QueryMetricsTools.class), "prometheusBaseUrl"))
                            .isEqualTo("http://127.0.0.1:19090");
                    assertThat(ReflectionTestUtils.getField(context.getBean(QueryMetricsTools.class), "timeout"))
                            .isEqualTo(3);
                });
    }
}
