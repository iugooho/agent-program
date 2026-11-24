package com.travelagent.travelplanner.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.application.EmailCodeHasher;
import com.travelagent.travelplanner.auth.domain.EmailCodeStore;
import com.travelagent.travelplanner.auth.domain.EmailPurpose;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** 个人中心安全、通知、导出与注销接口集成测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AccountCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmailCodeStore emailCodeStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("通知偏好可以读取默认值并更新")
    void readsAndUpdatesNotificationPreferences() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");

        mockMvc.perform(get("/api/v1/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripReminder").value(true))
                .andExpect(jsonPath("$.data.emailEnabled").value(true));

        mockMvc.perform(put("/api/v1/users/me/notification-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tripReminder": false,
                                  "tripChange": true,
                                  "systemMessage": false,
                                  "emailEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripReminder").value(false))
                .andExpect(jsonPath("$.data.systemMessage").value(false))
                .andExpect(jsonPath("$.data.emailEnabled").value(false));
    }

    @Test
    @DisplayName("可以列出并撤销其他登录会话，同时写入安全日志")
    void listsAndRevokesSessions() throws Exception {
        String currentBearer = bearerByLogin("demo", "Demo123456");
        String revokedBearer = bearerByLogin("demo", "Demo123456");

        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/users/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, currentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        JsonNode sessions = objectMapper.readTree(sessionsResult.getResponse().getContentAsString())
                .path("data");
        String targetSession = null;
        for (JsonNode session : sessions) {
            if (!session.path("current").asBoolean()) {
                targetSession = session.path("sessionId").asText();
                break;
            }
        }

        mockMvc.perform(delete("/api/v1/users/me/sessions/{sessionId}", targetSession)
                        .header(HttpHeaders.AUTHORIZATION, currentBearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, revokedBearer))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, currentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].current").value(true));

        mockMvc.perform(get("/api/v1/users/me/security-log")
                        .header(HttpHeaders.AUTHORIZATION, currentBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].eventType").value("SESSION_REVOKED"));
    }

    @Test
    @DisplayName("数据导出包含资料、偏好、通知设置和本人行程")
    void exportsOwnedData() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");

        mockMvc.perform(get("/api/v1/users/me/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.username").value("demo"))
                .andExpect(jsonPath("$.data.preferences").exists())
                .andExpect(jsonPath("$.data.notificationPreferences").exists())
                .andExpect(jsonPath("$.data.trips").isArray());
    }

    @Test
    @DisplayName("注销账号需要当前邮箱验证码并立即撤销全部会话")
    void deletesAccountAfterEmailVerification() throws Exception {
        String bearer = bearerByLogin("demo", "Demo123456");
        emailCodeStore.saveCode(
                "demo@example.com",
                EmailPurpose.UPDATE_PROFILE,
                EmailCodeHasher.hash("demo@example.com", EmailPurpose.UPDATE_PROFILE, "654321"),
                Duration.ofMinutes(5));

        mockMvc.perform(post("/api/v1/users/me/deletion")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "verificationCode", "654321"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.code()));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE username = 'deleted_1'",
                String.class);
        org.junit.jupiter.api.Assertions.assertEquals("DELETED", statusValue);
    }

    private String bearerByLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginType", "PASSWORD",
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        return "Bearer " + token;
    }
}
