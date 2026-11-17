package com.travelagent.travelplanner.rbac.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
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

    @Test
    @DisplayName("自定义角色支持创建、编辑、停用和删除")
    void customRoleLifecycle() throws Exception {
        Overview overview = overview();
        long tripReadPermissionId = overview.permissionId("trip.read");
        String code = "CUSTOM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                .toUpperCase();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code,
                                "name", "自定义角色",
                                "description", "生命周期测试",
                                "permissionIds", List.of(tripReadPermissionId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data");
        long roleId = created.path("id").asLong();
        assertEquals(code, created.path("code").asText());

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "已编辑角色",
                                "description", "编辑成功"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("已编辑角色"));

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/status", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(delete("/api/v1/admin/rbac/roles/{roleId}", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertFalse(overview().containsRole(roleId), "删除后的角色不应再出现在配置列表");
    }

    @Test
    @DisplayName("停用角色后用户权限立即失效")
    void disabledRoleLosesEffectivePermissions() throws Exception {
        Overview overview = overview();
        long tripReadPermissionId = overview.permissionId("trip.read");
        long demoUserId = overview.userId("demo");
        long userRoleId = overview.roleId("USER");
        String code = "PAUSED_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                .toUpperCase();
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code,
                                "name", "待停用角色",
                                "description", "验证即时失效",
                                "permissionIds", List.of(tripReadPermissionId)))))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/v1/admin/rbac/users/{userId}/roles", demoUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(userRoleId, roleId)))))
                .andExpect(status().isOk());
        String demoBearer = loginBearer("demo", "Demo123456");
        assertTrue(roleCodes(demoBearer).contains(code), "停用前角色应生效");

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/status", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk());

        assertFalse(roleCodes(demoBearer).contains(code), "停用后角色应立即失效");
    }

    @Test
    @DisplayName("内置角色禁止停用和删除")
    void builtInRolesAreProtected() throws Exception {
        Overview overview = overview();
        long userRoleId = overview.roleId("USER");

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/status", userRoleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.ROLE_PROTECTED.code()));

        mockMvc.perform(delete("/api/v1/admin/rbac/roles/{roleId}", userRoleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.ROLE_PROTECTED.code()));
    }

    private List<String> roleCodes(String bearer) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andReturn();
        List<String> roles = new ArrayList<>();
        objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("roles").forEach(role -> roles.add(role.asText()));
        return roles;
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

        long permissionId(String code) {
            for (JsonNode group : data.path("permissionGroups")) {
                for (JsonNode permission : group.path("permissions")) {
                    if (code.equals(permission.path("code").asText())) {
                        return permission.path("id").asLong();
                    }
                }
            }
            throw new IllegalArgumentException("权限不存在: " + code);
        }

        boolean containsRole(long roleId) {
            for (JsonNode role : data.path("roles")) {
                if (role.path("id").asLong() == roleId) {
                    return true;
                }
            }
            return false;
        }
    }
}
