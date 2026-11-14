package com.travelagent.travelplanner.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.domain.CaptchaGenerator;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 注册、登录、当前用户、刷新和退出的端到端接口测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(AuthIntegrationTest.CaptchaTestConfiguration.class)
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("本地用户完成登录、查询资料、刷新与退出闭环")
    void loginRefreshAndLogoutFlow() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginType":"PASSWORD","username":"demo","password":"Demo123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode tokenData = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data");
        String accessToken = tokenData.path("accessToken").asText();
        String refreshToken = tokenData.path("refreshToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.data.permissions[?(@ == 'trip.create')]").exists());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.code()));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.code()));
    }

    @Test
    @DisplayName("错误密码返回 1003 且不签发令牌")
    void wrongPasswordReturnsDocumentedCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginType":"PASSWORD","username":"demo","password":"Wrong123456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_CREDENTIALS.code()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("邮箱验证码登录按 LOGIN 用途消费验证码")
    void emailCodeLoginFlow() throws Exception {
        CaptchaCredentials captcha = requestCaptcha("EMAIL_CODE");
        mockMvc.perform(post("/api/v1/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "demo@example.com",
                                "purpose", "LOGIN",
                                "captchaChallengeId", captcha.challengeId(),
                                "captchaAnswer", captcha.answer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.expiresIn").value(300))
                .andExpect(jsonPath("$.data.retryAfterSeconds").value(60));

        String code = latestEmailCode("LOGIN", "demo@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginType":"EMAIL_CODE","email":"demo@example.com","emailCode":"%s"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("邮箱验证码完成注册后可直接查询资料并使用密码登录")
    void registerThenLoginWithPassword() throws Exception {
        String username = "reg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        String code = requestEmailCode(email, "REGISTER");

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "password", "Password123",
                                "email", email,
                                "emailCode", code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("USER"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginType":"PASSWORD","username":"%s","password":"Password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("错误注册验证码返回 1004")
    void registrationWithWrongCodeReturnsDocumentedCode() throws Exception {
        String username = "bad_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String email = username + "@example.com";
        String actualCode = requestEmailCode(email, "REGISTER");
        String wrongCode = "000000".equals(actualCode) ? "000001" : "000000";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "password", "Password123",
                                "email", email,
                                "emailCode", wrongCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_CODE_INVALID.code()));
    }

    @Test
    @DisplayName("首次密码失败后后续登录必须完成人机验证")
    void repeatedPasswordFailuresRequireCaptcha() throws Exception {
        String username = "risk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "loginType", "PASSWORD",
                "username", username,
                "password", "WrongPassword1"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_CREDENTIALS.code()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value(ErrorCode.CAPTCHA_REQUIRED.code()));
    }

    @Test
    @DisplayName("重复注册账号返回 1001")
    void duplicateRegistrationUsernameReturnsDocumentedCode() throws Exception {
        String email = "dup_name_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "@example.com";
        String code = requestEmailCode(email, "REGISTER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", "demo",
                                "password", "Password123",
                                "email", email,
                                "emailCode", code))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_EXISTS.code()));
    }

    @Test
    @DisplayName("重复注册邮箱返回 1002")
    void duplicateRegistrationEmailReturnsDocumentedCode() throws Exception {
        String username = "dup_mail_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String code = requestEmailCode("demo@example.com", "REGISTER");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "username", username,
                                "password", "Password123",
                                "email", "demo@example.com",
                                "emailCode", code))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_EXISTS.code()));
    }

    @Test
    @DisplayName("未登录访问用户资料返回 401 业务码")
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.code()));
    }

    private String requestEmailCode(String email, String purpose) throws Exception {
        CaptchaCredentials captcha = requestCaptcha("EMAIL_CODE");
        mockMvc.perform(post("/api/v1/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email,
                                "purpose", purpose,
                                "captchaChallengeId", captcha.challengeId(),
                                "captchaAnswer", captcha.answer()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        return latestEmailCode(purpose, email);
    }

    private CaptchaCredentials requestCaptcha(String purpose) throws Exception {
        MvcResult challengeResult = mockMvc.perform(post("/api/v1/auth/captcha/challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "purpose", purpose))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageDataUrl").isNotEmpty())
                .andReturn();
        String challengeId = objectMapper.readTree(challengeResult.getResponse().getContentAsString())
                .path("data")
                .path("challengeId")
                .asText();
        return new CaptchaCredentials(challengeId, "ABCDE");
    }

    private String latestEmailCode(String purpose, String email) throws Exception {
        return Files.readAllLines(Path.of("target", "local-email-outbox.log")).stream()
                .filter(line -> line.contains("| " + purpose + " | " + email + " |"))
                .reduce((previous, current) -> current)
                .map(line -> line.substring(line.lastIndexOf("| ") + 2).strip())
                .orElseThrow(() -> new IllegalStateException("测试验证码没有写入本地发件箱"));
    }

    /** 固定测试验证码，生产环境仍使用随机 SVG 验证码。 */
    @TestConfiguration(proxyBeanMethods = false)
    static class CaptchaTestConfiguration {

        @Bean
        @Primary
        CaptchaGenerator captchaGenerator() {
            return () -> new CaptchaGenerator.GeneratedCaptcha(
                    "ABCDE",
                    "data:image/svg+xml;base64,PHN2Zy8+");
        }
    }

    private record CaptchaCredentials(String challengeId, String answer) {
    }
}
