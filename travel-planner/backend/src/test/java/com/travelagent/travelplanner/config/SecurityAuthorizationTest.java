package com.travelagent.travelplanner.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.auth.application.TokenService;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** URL 级角色规则和方法级 RBAC 注解的集成测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(SecurityAuthorizationTest.SecurityProbeController.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Test
    @DisplayName("未登录访问后台路径返回 401")
    void anonymousUserCannotAccessAdminPath() throws Exception {
        assertNotNull(rateLimitFilter);
        mockMvc.perform(get("/api/v1/admin/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHENTICATED.code()));
    }

    @Test
    @DisplayName("普通用户访问后台路径返回 403")
    void standardUserCannotAccessAdminPath() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    @DisplayName("管理员可以访问后台路径")
    void adminCanAccessAdminPath() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ApiResponse.CODE_SUCCESS))
                .andExpect(jsonPath("$.data").value("admin"));
    }

    @Test
    @DisplayName("本地管理员账号可以访问真实后台校验接口")
    void localAdminCanAccessRealAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/access-check")
                        .header(HttpHeaders.AUTHORIZATION, bearerByLogin("admin", "Admin123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.accessGranted").value(true));
    }

    @Test
    @DisplayName("本地普通用户无法访问真实后台校验接口")
    void localStandardUserCannotAccessRealAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/access-check")
                        .header(HttpHeaders.AUTHORIZATION, bearerByLogin("demo", "Demo123456")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    @DisplayName("方法级 PreAuthorize 拒绝普通用户并允许管理员")
    void methodSecurityEnforcesAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/internal/security-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));

        mockMvc.perform(get("/api/v1/internal/security-probe")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("admin"));
    }

    private String bearer(String role) {
        UserAccount user = new UserAccount(
                1L,
                role.toLowerCase() + "-security-test",
                role.toLowerCase() + "-security-test@example.com",
                "unused-password-hash",
                role,
                "ACTIVE",
                "Security Test",
                null,
                LocalDateTime.now(),
                null);
        String token = tokenService.issue(user).accessToken();
        return "Bearer " + token;
    }

    private String bearerByLogin(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "loginType", "PASSWORD",
                "username", username,
                "password", password));
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).path("data").path("accessToken").asText();
        return "Bearer " + token;
    }

    /** 仅用于测试 URL 级和方法级 RBAC，不会注册到生产应用。 */
    @RestController
    static class SecurityProbeController {

        @GetMapping("/api/v1/admin/security-probe")
        ApiResponse<String> adminPath() {
            return ApiResponse.ok("admin");
        }

        @GetMapping("/api/v1/internal/security-probe")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<String> methodProtected() {
            return ApiResponse.ok("admin");
        }
    }
}
