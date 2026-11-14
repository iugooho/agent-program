package com.travelagent.travelplanner.rbac.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** 超级管理员权限分配接口集成测试。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class RbacAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("超级管理员可读取最小 RBAC 权限配置")
    void superAdminCanReadOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/rbac/overview")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(3))
                .andExpect(jsonPath("$.data.roles[0].code").value("USER"))
                .andExpect(jsonPath("$.data.roles[2].code").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.permissionGroups").isArray())
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    @DisplayName("普通用户不能进入权限分配接口")
    void standardUserCannotReadOverview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/rbac/overview")
                        .header(HttpHeaders.AUTHORIZATION, loginBearer("demo", "Demo123456")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.code()));
    }

    @Test
    @DisplayName("超级管理员可以给用户分配管理员角色")
    void superAdminCanAssignUserRole() throws Exception {
        Overview overview = overview();
        long demoUserId = overview.userId("demo");
        long userRoleId = overview.roleId("USER");
        long adminRoleId = overview.roleId("ADMIN");

        mockMvc.perform(put("/api/v1/admin/rbac/users/{userId}/roles", demoUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(userRoleId, adminRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("不能取消最后一个超级管理员")
    void cannotRemoveLastSuperAdmin() throws Exception {
        Overview overview = overview();
        long adminUserId = overview.userId("admin");
        long superAdminRoleId = overview.roleId("SUPER_ADMIN");

        mockMvc.perform(put("/api/v1/admin/rbac/users/{userId}/roles", adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(superAdminRoleId)))))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/rbac/users/{userId}/roles", adminUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roleIds", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("至少需要保留一个超级管理员"));
    }

    @Test
    @DisplayName("超级管理员角色权限固定，不能取消")
    void cannotUpdateSuperAdminPermissions() throws Exception {
        Overview overview = overview();
        long superAdminRoleId = overview.roleId("SUPER_ADMIN");

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/permissions", superAdminRoleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("超级管理员权限固定为全部权限"));
    }

    private Overview overview() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/rbac/overview")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andReturn();
        return new Overview(objectMapper.readTree(result.getResponse().getContentAsString()).path("data"));
    }

    private String adminBearer() throws Exception {
        return loginBearer("admin", "Admin123456");
    }

    private String loginBearer(String username, String password) throws Exception {
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

    /** 权限配置响应中的主键查询。 */
    private record Overview(JsonNode data) {

        long roleId(String code) {
            for (JsonNode role : data.path("roles")) {
                if (code.equals(role.path("code").asText())) {
                    return role.path("id").asLong();
                }
            }
            throw new IllegalArgumentException("角色不存在: " + code);
        }

        long userId(String username) {
            for (JsonNode user : data.path("users")) {
                if (username.equals(user.path("username").asText())) {
                    return user.path("id").asLong();
                }
            }
            throw new IllegalArgumentException("用户不存在: " + username);
        }
    }
}
