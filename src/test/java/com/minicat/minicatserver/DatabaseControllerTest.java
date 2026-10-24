package com.minicat.minicatserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.minicat.minicatserver.entity.DatabaseConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 契约测试：锁定 HTTP 状态码与 ApiResponseDTO 响应结构。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listConnectionsReturnsSuccessEnvelope() throws Exception {
        mockMvc.perform(get("/api/database/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createListAndDeleteConnection() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/database/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConnection("it-controller"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("it-controller"))
                .andReturn();

        String body = created.getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");

        // 列表接口必须屏蔽密码（字段为 null）
        mockMvc.perform(get("/api/database/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].password").value(nullValue()));

        mockMvc.perform(delete("/api/database/connections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateConnectionReturnsSuccess() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/database/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConnection("it-update"))))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        DatabaseConnection update = newConnection("it-update-renamed");
        update.setPassword(null);

        mockMvc.perform(put("/api/database/connections/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createConnectionWithoutNameReturns400() throws Exception {
        DatabaseConnection invalid = newConnection("x");
        invalid.setName(null);

        mockMvc.perform(post("/api/database/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private static DatabaseConnection newConnection(String name) {
        DatabaseConnection connection = new DatabaseConnection();
        connection.setName(name);
        connection.setHost("localhost");
        connection.setPort(3306);
        connection.setUsername("root");
        connection.setPassword("secret-123");
        connection.setDatabaseType("mysql");
        connection.setDatabaseName("demo");
        connection.setActive(true);
        return connection;
    }
}
