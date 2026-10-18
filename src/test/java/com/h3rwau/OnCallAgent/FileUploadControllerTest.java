package com.h3rwau.OnCallAgent;

import com.h3rwau.OnCallAgent.config.FileUploadConfig;
import com.h3rwau.OnCallAgent.controller.FileUploadController;
import com.h3rwau.OnCallAgent.service.VectorIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadControllerTest {
    @TempDir
    Path directory;
    private final VectorIndexService indexService = mock(VectorIndexService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var config = new FileUploadConfig();
        config.setPath(directory.toString());
        config.setAllowedExtensions("txt,md");
        var controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "fileUploadConfig", config);
        ReflectionTestUtils.setField(controller, "vectorIndexService", indexService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void successfulIndexingPreservesTheUploadContract() throws Exception {
        mvc.perform(multipart("/api/upload").file(document()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("runbook.md"));
    }

    @Test
    void failedIndexingIsReportedInsteadOfAnnouncingSuccess() throws Exception {
        doThrow(new IllegalStateException("embedding unavailable")).when(indexService).indexSingleFile(anyString());
        mvc.perform(multipart("/api/upload").file(document()))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("文件已保存，但向量索引失败，请检查后端日志后重新上传。"));
        assertThat(Files.exists(directory.resolve("runbook.md"))).isTrue();
    }

    private MockMultipartFile document() {
        return new MockMultipartFile("file", "runbook.md", "text/markdown", "# Runbook".getBytes());
    }
}
