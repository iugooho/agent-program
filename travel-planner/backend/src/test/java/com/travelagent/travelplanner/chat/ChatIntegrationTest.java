package com.travelagent.travelplanner.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 实时对话的端到端接口测试：创建会话、发送消息、异步生成、归属校验与删除。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ChatIntegrationTest {

    private static final String PASSWORD = "Chat123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("登录用户可以创建会话、发送消息并收到落库后的助手回复")
    void chatFlowPersistsAssistantReply() throws Exception {
        String token = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        long conversationId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertThat(conversationId).isPositive();

        MvcResult sent = mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"从北京去杭州玩 4 天，2 个人，预算 8000，不要购物"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reply.status").value("PENDING"))
                .andReturn();
        long replyId = objectMapper.readTree(sent.getResponse().getContentAsString())
                .path("data").path("reply").path("id").asLong();

        JsonNode reply = waitForReply(token, conversationId, replyId);
        assertThat(reply.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(reply.path("model").asText()).isEqualTo("rule");
        assertThat(reply.path("content").asText()).contains("杭州").contains("4 天");

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2))
                .andExpect(jsonPath("$.data.title").value("从北京去杭州玩 4 天，2 个人，预算 8000，不要购物"));

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasMore").value(false));

        mockMvc.perform(get("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].messageCount").value(2))
                .andExpect(jsonPath("$.data.items[0].lastMessagePreview").isNotEmpty());

        mockMvc.perform(delete("/api/v1/conversations/{id}", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("用户不能读取或写入他人的会话")
    void rejectsForeignConversation() throws Exception {
        String ownerToken = registerAndLogin();
        String otherToken = registerAndLogin();

        MvcResult created = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"私有行程\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long conversationId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4000));

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"越权写入\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4000));
    }

    private JsonNode waitForReply(String token, long conversationId, long replyId) throws Exception {
        long deadline = System.currentTimeMillis() + 8_000L;
        JsonNode lastReply = null;
        while (System.currentTimeMillis() < deadline) {
            MvcResult history = mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode items = objectMapper.readTree(history.getResponse().getContentAsString())
                    .path("data").path("items");
            for (JsonNode item : items) {
                if (item.path("id").asLong() == replyId) {
                    lastReply = item;
                }
            }
            if (lastReply != null && !"PENDING".equals(lastReply.path("status").asText())
                    && !"STREAMING".equals(lastReply.path("status").asText())) {
                return lastReply;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("助手回复未在超时时间内完成: " + lastReply);
    }

    private String registerAndLogin() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String username = "chat_" + suffix;
        String email = username + "@example.com";
        userRepository.createUser(username, email, passwordEncoder.encode(PASSWORD), "chat tester");

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "loginType", "PASSWORD",
                                "username", username,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
