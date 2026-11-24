package com.travelagent.travelplanner.rbac.infrastructure.persistence;

import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import com.travelagent.travelplanner.rbac.domain.RbacAuditLog;
import com.travelagent.travelplanner.rbac.domain.RbacDepartment;
import com.travelagent.travelplanner.rbac.domain.RbacPermission;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionDependency;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionPackage;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import com.travelagent.travelplanner.rbac.domain.RbacUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的 RBAC 仓储。 */
@Repository
public class MyBatisRbacRepository implements RbacRepository {

    private final RbacMapper mapper;

    /**
     * 创建 RBAC 仓储。
     *
     * @param mapper RBAC Mapper
     */
    public MyBatisRbacRepository(RbacMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public EffectiveAuthorization findEffectiveAuthorization(long userId) {
        return new EffectiveAuthorization(
                mapper.findRoleCodesByUser(userId),
                mapper.findPermissionCodesByUser(userId));
    }

    @Override
    public RbacSnapshot loadSnapshot() {
        List<RbacRole> roles = mapper.findRoles().stream()
                .map(row -> new RbacRole(
                        row.id(),
                        row.code(),
                        row.name(),
                        row.description(),
                        row.builtIn(),
                        row.status(),
                        row.dataScope()))
                .toList();
        List<RbacPermission> permissions = mapper.findPermissions().stream()
                .map(row -> new RbacPermission(
                        row.id(),
                        row.parentId(),
                        row.code(),
                        row.name(),
                        row.module(),
                        row.type(),
                        row.routePath(),
                        row.description(),
                        row.status(),
                        row.riskLevel(),
                        row.builtIn()))
                .toList();
        List<RbacPermissionPackage> permissionPackages = mapper.findPermissionPackages().stream()
                .map(row -> new RbacPermissionPackage(
                        row.id(),
                        row.code(),
                        row.name(),
                        row.description(),
                        row.status()))
                .toList();
        List<RbacUser> users = mapper.findUsers().stream()
                .map(row -> new RbacUser(
                        row.id(), row.username(), row.nickname(), row.email(), row.status()))
                .toList();
        List<RbacDepartment> departments = mapper.findDepartments().stream()
                .map(row -> new RbacDepartment(
                        row.id(),
                        row.parentId(),
                        row.code(),
                        row.name(),
                        row.leaderUserId(),
                        row.status(),
                        row.sortOrder()))
                .toList();
        Map<Long, List<Long>> rolePermissionIds = mapper.findRolePermissions().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        Map<Long, List<Long>> userRoleIds = mapper.findUserRoles().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        Map<Long, List<Long>> userDepartmentIds = mapper.findUserDepartments().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        Map<Long, List<Long>> roleDataScopeDepartmentIds =
                mapper.findRoleDataScopeDepartments().stream()
                        .collect(Collectors.groupingBy(
                                RbacRows.AssignmentRow::ownerId,
                                Collectors.mapping(
                                        RbacRows.AssignmentRow::targetId,
                                        Collectors.toList())));
        Map<Long, List<Long>> packagePermissionIds = mapper.findPackagePermissions().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        List<RbacPermissionDependency> permissionDependencies =
                mapper.findPermissionDependencies().stream()
                        .map(row -> new RbacPermissionDependency(
                                row.permissionId(),
                                row.requiredPermissionId()))
                        .toList();
        return new RbacSnapshot(
                roles,
                permissions,
                permissionPackages,
                users,
                rolePermissionIds,
                packagePermissionIds,
                permissionDependencies,
                departments,
                userRoleIds,
                userDepartmentIds,
                roleDataScopeDepartmentIds);
    }

    @Override
    public Optional<RbacRole> findRoleById(long roleId) {
        return Optional.ofNullable(mapper.findRoleById(roleId)).map(this::toDomain);
    }

    @Override
    public Optional<RbacRole> findRoleByCode(String code) {
        return Optional.ofNullable(mapper.findRoleByCode(code)).map(this::toDomain);
    }

    @Override
    public Optional<RbacPermission> findPermissionById(long permissionId) {
        return Optional.ofNullable(mapper.findPermissionById(permissionId))
                .map(this::toDomain);
    }

    @Override
    public Optional<RbacDepartment> findDepartmentById(long departmentId) {
        return Optional.ofNullable(mapper.findDepartmentById(departmentId))
                .map(this::toDomain);
    }

    @Override
    public RbacDepartment createDepartment(
            Long parentId,
            String code,
            String name,
            Long leaderUserId,
            int sortOrder) {
        mapper.insertDepartment(parentId, code, name, leaderUserId, sortOrder);
        return Optional.ofNullable(mapper.findDepartmentByCode(code))
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("部门创建后未能查询到"));
    }

    @Override
    public void updateDepartment(
            long departmentId,
            Long parentId,
            String name,
            Long leaderUserId,
            String status,
            int sortOrder) {
        mapper.updateDepartment(
                departmentId,
                parentId,
                name,
                leaderUserId,
                status,
                sortOrder);
    }

    @Override
    public void deleteDepartment(long departmentId) {
        mapper.markDepartmentDeleted(departmentId);
    }

    @Override
    public int countActiveDepartmentChildren(long departmentId) {
        return mapper.countActiveDepartmentChildren(departmentId);
    }

    @Override
    public RbacRole createRole(String code, String name, String description) {
        mapper.insertRole(code, name, description);
        return findRoleByCode(code)
                .orElseThrow(() -> new IllegalStateException("角色创建后未能查询到"));
    }

    @Override
    public RbacPermission createPermission(
            String code,
            String name,
            String module,
            String type,
            Long parentId,
            String routePath,
            String riskLevel,
            String description) {
        int sortOrder = mapper.findMaxPermissionSortOrder() + 10;
        mapper.insertPermission(
                code,
                name,
                module,
                type,
                parentId,
                routePath,
                riskLevel,
                description,
                sortOrder);
        return Optional.ofNullable(mapper.findPermissionByCode(code))
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("权限创建后未能查询到"));
    }

    @Override
    public void updateRole(long roleId, String name, String description) {
        mapper.updateRole(roleId, name, description);
    }

    @Override
    public void updateRoleStatus(long roleId, String status) {
        mapper.updateRoleStatus(roleId, status);
    }

    @Override
    public void deleteRole(long roleId) {
        mapper.deleteRoleUsers(roleId);
        mapper.deleteRolePermissions(roleId);
        mapper.markRoleDeleted(roleId);
    }

    @Override
    public void deletePermission(long permissionId) {
        mapper.markPermissionDeleted(permissionId);
    }

    @Override
    public int countActivePermissionChildren(long permissionId) {
        return mapper.countActivePermissionChildren(permissionId);
    }

    @Override
    public void assignUsersToRole(long roleId, List<Long> userIds) {
        if (!userIds.isEmpty()) {
            mapper.insertUsersForRole(roleId, userIds);
        }
    }

    @Override
    public List<Long> findUserIdsByRole(long roleId) {
        return mapper.findUserIdsByRole(roleId);
    }

    @Override
    public void assignPermissionToRole(long roleId, long permissionId) {
        mapper.insertRolePermission(roleId, permissionId);
    }

    @Override
    public void replaceRolePermissions(long roleId, List<Long> permissionIds) {
        mapper.deleteRolePermissions(roleId);
        if (!permissionIds.isEmpty()) {
            mapper.insertRolePermissions(roleId, permissionIds);
        }
    }

    @Override
    public void replaceUserRoles(long userId, List<Long> roleIds) {
        mapper.deleteUserRoles(userId);
        if (!roleIds.isEmpty()) {
            mapper.insertUserRoles(userId, roleIds);
        }
    }

    @Override
    public void appendAuditLog(RbacAuditLog auditLog) {
        mapper.insertAuditLog(new RbacRows.AuditLogRow(
                0,
                auditLog.operatorId(),
                auditLog.operatorName(),
                auditLog.targetType(),
                auditLog.targetId(),
                auditLog.action(),
                auditLog.beforeValue(),
                auditLog.afterValue(),
                auditLog.clientIp(),
                null));
    }

    @Override
    public List<RbacAuditLog> findAuditLogs(int limit) {
        return mapper.findAuditLogs(limit).stream()
                .map(row -> new RbacAuditLog(
                        row.id(),
                        row.operatorId(),
                        row.operatorName(),
                        row.targetType(),
                        row.targetId(),
                        row.action(),
                        row.beforeValue(),
                        row.afterValue(),
                        row.clientIp(),
                        row.createTime()))
                .toList();
    }

    @Override
    public void replaceUserDepartments(long userId, List<Long> departmentIds) {
        mapper.deleteUserDepartments(userId);
        if (!departmentIds.isEmpty()) {
            mapper.insertUserDepartments(userId, departmentIds);
        }
    }

    @Override
    public void replaceRoleDataScope(
            long roleId,
            String dataScope,
            List<Long> departmentIds) {
        mapper.updateRoleDataScope(roleId, dataScope);
        mapper.deleteRoleDataScopeDepartments(roleId);
        if (!departmentIds.isEmpty()) {
            mapper.insertRoleDataScopeDepartments(roleId, departmentIds);
        }
    }

    private RbacRole toDomain(RbacRows.RoleRow row) {
        return new RbacRole(
                row.id(),
                row.code(),
                row.name(),
                row.description(),
                row.builtIn(),
                row.status(),
                row.dataScope());
    }

    private RbacPermission toDomain(RbacRows.PermissionRow row) {
        return new RbacPermission(
                row.id(),
                row.parentId(),
                row.code(),
                row.name(),
                row.module(),
                row.type(),
                row.routePath(),
                row.description(),
                row.status(),
                row.riskLevel(),
                row.builtIn());
    }

    private RbacDepartment toDomain(RbacRows.DepartmentRow row) {
        return new RbacDepartment(
                row.id(),
                row.parentId(),
                row.code(),
                row.name(),
                row.leaderUserId(),
                row.status(),
                row.sortOrder());
    }
}
