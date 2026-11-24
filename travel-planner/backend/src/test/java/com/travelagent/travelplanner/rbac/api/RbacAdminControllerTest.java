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
                .andExpect(jsonPath("$.data.permissionGroups[0].permissions[0].builtIn").value(true))
                .andExpect(jsonPath("$.data.departments.length()").value(3))
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    @DisplayName("超级管理员可创建和删除自定义权限点")
    void customPermissionLifecycle() throws Exception {
        String code = "custom."
                + "p"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase();
        String body = objectMapper.writeValueAsString(Map.of(
                "code", code,
                "name", "自定义导出",
                "module", "扩展功能",
                "type", "BUTTON",
                "parentId", overview().permissionId("catalog.rbac"),
                "riskLevel", "LOW",
                "description", "导出业务报表"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.module").value("权限管理"))
                .andExpect(jsonPath("$.data.type").value("BUTTON"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.builtIn").value(false))
                .andReturn();
        long permissionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
        assertTrue(overview().superAdminHasPermission(permissionId), "新权限应自动授予超级管理员");

        mockMvc.perform(post("/api/v1/admin/rbac/permissions")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.PERMISSION_CODE_EXISTS.code()));

        mockMvc.perform(delete("/api/v1/admin/rbac/permissions/{permissionId}", permissionId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        assertFalse(overview().containsPermission(code), "删除后的权限不应再出现在配置列表");
    }

    @Test
    @DisplayName("内置权限禁止删除")
    void builtInPermissionCannotBeDeleted() throws Exception {
        long permissionId = overview().permissionId("admin.rbac.manage");

        mockMvc.perform(delete("/api/v1/admin/rbac/permissions/{permissionId}", permissionId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PERMISSION_PROTECTED.code()));
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
    @DisplayName("自定义角色仅凭后台权限码即可进入后台")
    void customRoleCanAccessAdminByPermissionCode() throws Exception {
        Overview overview = overview();
        long consolePermissionId = overview.permissionId("admin.console.access");
        long demoUserId = overview.userId("demo");
        String roleCode = "CONSOLE_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", roleCode,
                                "name", "后台入口角色",
                                "description", "只拥有后台入口权限",
                                "permissionIds", List.of(consolePermissionId)))))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(put("/api/v1/admin/rbac/users/{userId}/roles", demoUserId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(roleId)))))
                .andExpect(status().isOk());

        String bearer = loginBearer("demo", "Demo123456");
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value(roleCode));

        mockMvc.perform(get("/api/v1/admin/access-check")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessGranted").value(true));
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

        mockMvc.perform(get("/api/v1/admin/rbac/audit")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].action").value("USER_ROLES_UPDATE"))
                .andExpect(jsonPath("$.data[0].operatorName").value("admin"));
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
        long tripDeletePermissionId = overview.permissionId("trip.delete");
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
                                "permissionIds", List.of(tripDeletePermissionId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data");
        long roleId = created.path("id").asLong();
        assertEquals(code, created.path("code").asText());
        List<Long> assignedPermissionIds = new ArrayList<>();
        created.path("permissionIds").forEach(id -> assignedPermissionIds.add(id.asLong()));
        assertTrue(
                assignedPermissionIds.containsAll(
                        List.of(tripDeletePermissionId, tripReadPermissionId)),
                "创建角色时应自动补齐依赖权限");

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

    @Test
    @DisplayName("新增高风险权限必须二次确认")
    void highRiskPermissionRequiresConfirmation() throws Exception {
        Overview overview = overview();
        long adminUserReadId = overview.permissionId("admin.user.read");
        String roleCode = "RISK_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", roleCode,
                                "name", "高风险测试角色",
                                "description", "",
                                "permissionIds", List.of()))))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/permissions", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "permissionIds", List.of(adminUserReadId),
                                "confirmHighRisk", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("包含新增高风险权限，请确认后保存"));

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/permissions", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "permissionIds", List.of(adminUserReadId),
                                "confirmHighRisk", true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("角色可复制权限和成员")
    void roleCanBeCopiedWithPermissionsAndMembers() throws Exception {
        Overview overview = overview();
        long userRoleId = overview.roleId("USER");
        String roleCode = "COPY_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        mockMvc.perform(post("/api/v1/admin/rbac/roles/{roleId}/copy", userRoleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", roleCode,
                                "name", "复制角色",
                                "description", "复制测试",
                                "copyPermissions", true,
                                "copyMembers", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(roleCode))
                .andExpect(jsonPath("$.data.permissionIds").isNotEmpty())
                .andExpect(jsonPath("$.data.userCount").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("部门支持创建、成员维护和删除")
    void departmentLifecycle() throws Exception {
        Overview overview = overview();
        long parentId = overview.departmentId("HEADQUARTERS");
        long demoUserId = overview.userId("demo");
        String departmentCode = "TEST_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/departments")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "parentId", parentId,
                                "code", departmentCode,
                                "name", "测试部门",
                                "sortOrder", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(departmentCode))
                .andReturn();
        long departmentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(put("/api/v1/admin/rbac/departments/{departmentId}", departmentId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "parentId", parentId,
                                "name", "测试部门-更新",
                                "status", "ACTIVE",
                                "sortOrder", 110))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试部门-更新"));

        mockMvc.perform(put("/api/v1/admin/rbac/departments/{departmentId}/members", departmentId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userIds", List.of(demoUserId)))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/rbac/departments/{departmentId}", departmentId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("角色可设置自定义数据范围")
    void roleDataScopeCanBeUpdated() throws Exception {
        Overview overview = overview();
        long departmentId = overview.departmentId("TECH");
        String roleCode = "SCOPE_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/rbac/roles")
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", roleCode,
                                "name", "范围角色",
                                "description", "数据范围测试",
                                "permissionIds", List.of()))))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(put("/api/v1/admin/rbac/roles/{roleId}/data-scope", roleId)
                        .header(HttpHeaders.AUTHORIZATION, adminBearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "dataScope", "DEPT_CUSTOM",
                                "departmentIds", List.of(departmentId)))))
                .andExpect(status().isOk());

        JsonNode role = overview().role(roleId);
        assertEquals("DEPT_CUSTOM", role.path("dataScope").asText());
        assertEquals(departmentId, role.path("dataScopeDepartmentIds").get(0).asLong());
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

        long departmentId(String code) {
            for (JsonNode department : data.path("departments")) {
                if (code.equals(department.path("code").asText())) {
                    return department.path("id").asLong();
                }
            }
            throw new IllegalArgumentException("部门不存在: " + code);
        }

        JsonNode role(long roleId) {
            for (JsonNode role : data.path("roles")) {
                if (role.path("id").asLong() == roleId) {
                    return role;
                }
            }
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }

        boolean containsRole(long roleId) {
            for (JsonNode role : data.path("roles")) {
                if (role.path("id").asLong() == roleId) {
                    return true;
                }
            }
            return false;
        }

        boolean containsPermission(String code) {
            for (JsonNode group : data.path("permissionGroups")) {
                for (JsonNode permission : group.path("permissions")) {
                    if (code.equals(permission.path("code").asText())) {
                        return true;
                    }
                }
            }
            return false;
        }

        boolean superAdminHasPermission(long permissionId) {
            for (JsonNode role : data.path("roles")) {
                if (!"SUPER_ADMIN".equals(role.path("code").asText())) {
                    continue;
                }
                for (JsonNode id : role.path("permissionIds")) {
                    if (id.asLong() == permissionId) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
