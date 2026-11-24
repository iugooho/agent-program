package com.travelagent.travelplanner.rbac.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.rbac.domain.RbacDataScope;
import com.travelagent.travelplanner.rbac.domain.RbacDepartment;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 数据范围规则测试。 */
class RbacDataScopeServiceTest {

    private final RbacRepository repository = mock(RbacRepository.class);
    private final RbacDataScopeService service = new RbacDataScopeService(repository);

    @Test
    @DisplayName("SELF 只允许访问本人资源")
    void selfScopeOnlyAllowsOwner() {
        when(repository.loadSnapshot()).thenReturn(snapshot(
                role(1, "USER", "SELF", "ACTIVE"),
                Map.of(7L, List.of(1L)),
                Map.of(7L, List.of(10L)),
                Map.of()));

        RbacDataScope scope = service.resolve(7);

        assertFalse(scope.allData());
        assertTrue(scope.selfAllowed());
        assertTrue(scope.departmentIds().isEmpty());
        assertTrue(service.canAccess(7, 7, 10L));
        assertFalse(service.canAccess(7, 8, 10L));
    }

    @Test
    @DisplayName("DEPT_ONLY 只允许本部门，不包含子部门")
    void departmentOnlyScopeExcludesChildren() {
        when(repository.loadSnapshot()).thenReturn(snapshot(
                role(1, "DEPT_ADMIN", "DEPT_ONLY", "ACTIVE"),
                Map.of(7L, List.of(1L)),
                Map.of(7L, List.of(20L)),
                Map.of()));

        RbacDataScope scope = service.resolve(7);

        assertEquals(Set.of(20L), scope.departmentIds());
        assertTrue(service.canAccess(7, 8, 20L));
        assertFalse(service.canAccess(7, 8, 21L));
        assertFalse(service.canAccess(7, 7, 21L));
    }

    @Test
    @DisplayName("DEPT_AND_CHILD 包含当前部门及所有下级部门")
    void departmentAndChildScopeIncludesDescendants() {
        when(repository.loadSnapshot()).thenReturn(snapshot(
                role(1, "ADMIN", "DEPT_AND_CHILD", "ACTIVE"),
                Map.of(7L, List.of(1L)),
                Map.of(7L, List.of(20L)),
                Map.of()));

        RbacDataScope scope = service.resolve(7);

        assertEquals(Set.of(20L, 21L, 22L), scope.departmentIds());
        assertTrue(service.canAccess(7, 8, 21L));
        assertTrue(service.canAccess(7, 9, 22L));
        assertFalse(service.canAccess(7, 8, 30L));
    }

    @Test
    @DisplayName("DEPT_CUSTOM 使用角色指定部门")
    void customScopeUsesConfiguredDepartments() {
        when(repository.loadSnapshot()).thenReturn(snapshot(
                role(1, "AUDITOR", "DEPT_CUSTOM", "ACTIVE"),
                Map.of(7L, List.of(1L)),
                Map.of(7L, List.of(20L)),
                Map.of(1L, List.of(30L))));

        RbacDataScope scope = service.resolve(7);

        assertEquals(Set.of(30L), scope.departmentIds());
        assertTrue(service.canAccess(7, 8, 30L));
        assertFalse(service.canAccess(7, 8, 20L));
    }

    @Test
    @DisplayName("全部数据与停用角色按预期生效")
    void allDataWinsAndDisabledRolesAreIgnored() {
        when(repository.loadSnapshot()).thenReturn(snapshot(
                role(1, "SUPER_ADMIN", "ALL", "ACTIVE"),
                role(2, "DISABLED_ADMIN", "DEPT_AND_CHILD", "DISABLED"),
                Map.of(7L, List.of(1L, 2L)),
                Map.of(7L, List.of(20L)),
                Map.of()));

        RbacDataScope scope = service.resolve(7);

        assertTrue(scope.allData());
        assertEquals(Set.of(), scope.departmentIds());
        assertTrue(service.canAccess(7, 99, 99L));
    }

    private RbacRole role(long id, String code, String dataScope, String status) {
        return new RbacRole(id, code, code, "", true, status, dataScope);
    }

    private RbacSnapshot snapshot(
            RbacRole role,
            Map<Long, List<Long>> userRoleIds,
            Map<Long, List<Long>> userDepartmentIds,
            Map<Long, List<Long>> roleDataScopeDepartmentIds) {
        return snapshot(
                role,
                null,
                userRoleIds,
                userDepartmentIds,
                roleDataScopeDepartmentIds);
    }

    private RbacSnapshot snapshot(
            RbacRole firstRole,
            RbacRole secondRole,
            Map<Long, List<Long>> userRoleIds,
            Map<Long, List<Long>> userDepartmentIds,
            Map<Long, List<Long>> roleDataScopeDepartmentIds) {
        List<RbacRole> roles = secondRole == null
                ? List.of(firstRole)
                : List.of(firstRole, secondRole);
        return new RbacSnapshot(
                roles,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                List.of(
                        new RbacDepartment(10, null, "HEADQUARTERS", "总部", null, "ACTIVE", 10),
                        new RbacDepartment(20, 10L, "TECH", "技术部", null, "ACTIVE", 20),
                        new RbacDepartment(21, 20L, "BACKEND", "后端组", null, "ACTIVE", 30),
                        new RbacDepartment(22, 21L, "PLATFORM", "平台组", null, "ACTIVE", 40),
                        new RbacDepartment(30, 10L, "OPS", "运营部", null, "ACTIVE", 50)),
                userRoleIds,
                userDepartmentIds,
                roleDataScopeDepartmentIds);
    }
}
