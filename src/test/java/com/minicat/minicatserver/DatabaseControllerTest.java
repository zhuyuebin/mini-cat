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
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void unknownConnectionReturns404WithBusinessCode() throws Exception {
        mockMvc.perform(get("/api/database/connections/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(delete("/api/database/connections/not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(put("/api/database/connections/not-exists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConnection("x"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void blockedSqlReturns403WithBusinessCode() throws Exception {
        String id = createConnection("it-blocked-sql");

        mockMvc.perform(post("/api/database/connections/{id}/query", id)
                        .param("database", "demo")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("SELECT * FROM users UNION SELECT * FROM passwords"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void unsupportedUpdateTypeReturns400WithBusinessCode() throws Exception {
        String id = createConnection("it-unsupported-update");

        mockMvc.perform(post("/api/database/connections/{id}/update", id)
                        .param("database", "demo")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("ALTER TABLE users ADD COLUMN age INT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void tableDataPageValidatesConnectionAndTableName() throws Exception {
        mockMvc.perform(get("/api/database/connections/not-exists/table-data")
                        .param("database", "demo")
                        .param("table", "users"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        String id = createConnection("it-table-data");
        mockMvc.perform(get("/api/database/connections/{id}/table-data", id)
                        .param("database", "demo")
                        .param("table", "users; DROP TABLE x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void uiSettingsExposeInlineEditFlag() throws Exception {
        mockMvc.perform(get("/api/database/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.inlineEditEnabled").value(true));
    }

    @Test
    void updateRowValidatesConnectionAndIdentifiers() throws Exception {
        String rowBody = "{\"table\":\"users\",\"keys\":{\"id\":1},\"changes\":{\"name\":\"a\"}}";

        mockMvc.perform(put("/api/database/connections/not-exists/table-row")
                        .param("database", "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rowBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        String id = createConnection("it-row-update");

        // 非法表名必须在触达数据库之前被拒绝（标识符白名单）
        mockMvc.perform(put("/api/database/connections/{id}/table-row", id)
                        .param("database", "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"table\":\"users; DROP TABLE x\",\"keys\":{\"id\":1},\"changes\":{\"name\":\"a\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));

        // 空 changes 直接 400，不依赖目标数据库
        mockMvc.perform(put("/api/database/connections/{id}/table-row", id)
                        .param("database", "demo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"table\":\"users\",\"keys\":{\"id\":1},\"changes\":{}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private String createConnection(String name) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/database/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConnection(name))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");
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
