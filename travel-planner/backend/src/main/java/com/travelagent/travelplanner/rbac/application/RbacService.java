package com.travelagent.travelplanner.rbac.application;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.rbac.api.RbacOverviewResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionGroupResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionResponse;
import com.travelagent.travelplanner.rbac.api.RbacRoleResponse;
import com.travelagent.travelplanner.rbac.api.RbacUserResponse;
import com.travelagent.travelplanner.rbac.domain.RbacPermission;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 超级管理员权限分配用例。 */
@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String PERMISSION_RBAC_MANAGE = "admin.rbac.manage";

    private final RbacRepository repository;

    /**
     * 创建 RBAC 服务。
     *
     * @param repository RBAC 仓储
     */
    public RbacService(RbacRepository repository) {
        this.repository = repository;
    }

    /**
     * 读取权限分配页面数据。
     *
     * @return 角色、权限和用户
     */
    @Transactional(readOnly = true)
    public RbacOverviewResponse overview() {
        return toResponse(repository.loadSnapshot());
    }

    /**
     * 更新角色权限。
     *
     * @param roleId 角色主键
     * @param permissionIds 权限主键
     */
    @Transactional
    public void updateRolePermissions(long roleId, List<Long> permissionIds) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacRole role = findRole(snapshot.roles(), roleId);
        if (ROLE_SUPER_ADMIN.equals(role.code())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "超级管理员权限固定为全部权限");
        }
        Set<Long> normalizedIds = normalize(permissionIds);
        Set<Long> availablePermissionIds = snapshot.permissions().stream()
                .map(RbacPermission::id)
                .collect(Collectors.toSet());
        if (!availablePermissionIds.containsAll(normalizedIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在的权限");
        }
        Long rbacManageId = snapshot.permissions().stream()
                .filter(permission -> PERMISSION_RBAC_MANAGE.equals(permission.code()))
                .map(RbacPermission::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少权限管理权限点"));
        if (normalizedIds.contains(rbacManageId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "权限管理只能由超级管理员角色拥有");
        }
        repository.replaceRolePermissions(roleId, List.copyOf(normalizedIds));
        log.info("RBAC role permission updated role={} permissions={}", role.code(), normalizedIds);
    }

    /**
     * 更新用户角色。
     *
     * @param userId 用户主键
     * @param roleIds 角色主键
     */
    @Transactional
    public void updateUserRoles(long userId, List<Long> roleIds) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        boolean userExists = snapshot.users().stream().anyMatch(user -> user.id() == userId);
        if (!userExists) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        Set<Long> normalizedIds = normalize(roleIds);
        Map<Long, RbacRole> roleById = snapshot.roles().stream()
                .collect(Collectors.toMap(RbacRole::id, role -> role));
        if (!roleById.keySet().containsAll(normalizedIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在的角色");
        }
        RbacRole superAdminRole = snapshot.roles().stream()
                .filter(role -> ROLE_SUPER_ADMIN.equals(role.code()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少超级管理员角色"));
        List<Long> currentRoleIds = snapshot.userRoleIds().getOrDefault(userId, List.of());
        boolean wasSuperAdmin = currentRoleIds.contains(superAdminRole.id());
        boolean remainsSuperAdmin = normalizedIds.contains(superAdminRole.id());
        if (wasSuperAdmin && !remainsSuperAdmin && countSuperAdmins(snapshot) <= 1) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "至少需要保留一个超级管理员");
        }
        repository.replaceUserRoles(userId, List.copyOf(normalizedIds));
        log.info("RBAC user role updated user={} roles={}", userId, normalizedIds);
    }

    private RbacOverviewResponse toResponse(RbacSnapshot snapshot) {
        Map<Long, Integer> userCounts = snapshot.users().stream()
                .flatMap(user -> snapshot.userRoleIds()
                        .getOrDefault(user.id(), List.of())
                        .stream())
                .collect(Collectors.groupingBy(
                        roleId -> roleId,
                        Collectors.summingInt(roleId -> 1)));
        List<RbacRoleResponse> roles = snapshot.roles().stream()
                .map(role -> new RbacRoleResponse(
                        role.id(),
                        role.code(),
                        role.name(),
                        role.description(),
                        role.builtIn(),
                        userCounts.getOrDefault(role.id(), 0),
                        snapshot.rolePermissionIds().getOrDefault(role.id(), List.of())))
                .toList();

        Map<String, List<RbacPermissionResponse>> grouped = new LinkedHashMap<>();
        for (RbacPermission permission : snapshot.permissions()) {
            grouped.computeIfAbsent(permission.module(), key -> new java.util.ArrayList<>())
                    .add(new RbacPermissionResponse(
                            permission.id(),
                            permission.code(),
                            permission.name(),
                            permission.description()));
        }
        List<RbacPermissionGroupResponse> permissionGroups = grouped.entrySet().stream()
                .map(entry -> new RbacPermissionGroupResponse(entry.getKey(), entry.getValue()))
                .toList();

        Map<Long, String> roleCodes = snapshot.roles().stream()
                .collect(Collectors.toMap(RbacRole::id, RbacRole::code));
        List<RbacUserResponse> users = snapshot.users().stream()
                .map(user -> {
                    List<Long> assignedRoleIds =
                            snapshot.userRoleIds().getOrDefault(user.id(), List.of());
                    List<String> assignedRoleCodes = assignedRoleIds.stream()
                            .map(roleCodes::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    return new RbacUserResponse(
                            user.id(),
                            user.username(),
                            user.nickname(),
                            user.email(),
                            user.status(),
                            assignedRoleIds,
                            assignedRoleCodes);
                })
                .toList();
        return new RbacOverviewResponse(roles, permissionGroups, users);
    }

    private RbacRole findRole(List<RbacRole> roles, long roleId) {
        return roles.stream()
                .filter(role -> role.id() == roleId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在"));
    }

    private long countSuperAdmins(RbacSnapshot snapshot) {
        long superAdminRoleId = snapshot.roles().stream()
                .filter(role -> ROLE_SUPER_ADMIN.equals(role.code()))
                .map(RbacRole::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少超级管理员角色"));
        return snapshot.userRoleIds().values().stream()
                .filter(roleIds -> roleIds.contains(superAdminRoleId))
                .count();
    }

    private Set<Long> normalize(List<Long> ids) {
        return new LinkedHashSet<>(ids);
    }
}
