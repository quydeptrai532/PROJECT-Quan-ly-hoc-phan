package com.edu.registration.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử an toàn khi OpenAPI và Swagger UI bị vô hiệu hóa trong môi trường Production (QLSV-101 / TASK-OP-07A).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenApiDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Khi vô hiệu hóa, truy cập /v3/api-docs phải trả về lỗi an toàn HTTP 404 Not Found")
    void apiDocs_whenDisabled_shouldReturnSafeNotFoundResponse() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Tài liệu API không khả dụng")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("Khi vô hiệu hóa, truy cập /swagger-ui.html phải trả về lỗi an toàn HTTP 404 Not Found")
    void swaggerUi_whenDisabled_shouldReturnSafeNotFoundResponse() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Tài liệu API không khả dụng")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Khi vô hiệu hóa, truy cập /swagger-ui/index.html phải trả về lỗi an toàn HTTP 404 Not Found")
    void swaggerUiIndex_whenDisabled_shouldReturnSafeNotFoundResponse() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Tài liệu API không khả dụng")));
    }
}
