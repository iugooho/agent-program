package com.travelagent.travelplanner.rbac.infrastructure.persistence;

import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import com.travelagent.travelplanner.rbac.domain.RbacPermission;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import com.travelagent.travelplanner.rbac.domain.RbacUser;
import java.util.List;
import java.util.Map;
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
                        row.id(), row.code(), row.name(), row.description(), row.builtIn()))
                .toList();
        List<RbacPermission> permissions = mapper.findPermissions().stream()
                .map(row -> new RbacPermission(
                        row.id(), row.code(), row.name(), row.module(), row.description()))
                .toList();
        List<RbacUser> users = mapper.findUsers().stream()
                .map(row -> new RbacUser(
                        row.id(), row.username(), row.nickname(), row.email(), row.status()))
                .toList();
        Map<Long, List<Long>> rolePermissionIds = mapper.findRolePermissions().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        Map<Long, List<Long>> userRoleIds = mapper.findUserRoles().stream()
                .collect(Collectors.groupingBy(
                        RbacRows.AssignmentRow::ownerId,
                        Collectors.mapping(RbacRows.AssignmentRow::targetId, Collectors.toList())));
        return new RbacSnapshot(roles, permissions, users, rolePermissionIds, userRoleIds);
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
}
