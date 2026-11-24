package com.travelagent.travelplanner.rbac.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.rbac.api.PermissionCreateRequest;
import com.travelagent.travelplanner.rbac.api.DepartmentCreateRequest;
import com.travelagent.travelplanner.rbac.api.DepartmentUpdateRequest;
import com.travelagent.travelplanner.rbac.api.RbacAuditLogResponse;
import com.travelagent.travelplanner.rbac.api.RbacDepartmentResponse;
import com.travelagent.travelplanner.rbac.api.RbacOverviewResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionDependencyResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionGroupResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionPackageResponse;
import com.travelagent.travelplanner.rbac.api.RbacPermissionResponse;
import com.travelagent.travelplanner.rbac.api.RbacRoleResponse;
import com.travelagent.travelplanner.rbac.api.RbacUserResponse;
import com.travelagent.travelplanner.rbac.api.RoleCreateRequest;
import com.travelagent.travelplanner.rbac.api.RoleCopyRequest;
import com.travelagent.travelplanner.rbac.api.RoleDataScopeRequest;
import com.travelagent.travelplanner.rbac.api.RoleUpdateRequest;
import com.travelagent.travelplanner.rbac.domain.RbacAuditLog;
import com.travelagent.travelplanner.rbac.domain.RbacDepartment;
import com.travelagent.travelplanner.rbac.domain.RbacPermission;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionVersionStore;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

/** 超级管理员权限分配用例。 */
@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String PERMISSION_RBAC_MANAGE = "admin.rbac.manage";
    private static final String TARGET_PERMISSION = "PERMISSION";
    private static final String TARGET_ROLE = "ROLE";
    private static final String TARGET_USER = "USER";
    private static final String TARGET_DEPARTMENT = "DEPARTMENT";
    private static final Set<String> DATA_SCOPES = Set.of(
            "ALL",
            "DEPT_CUSTOM",
            "DEPT_ONLY",
            "DEPT_AND_CHILD",
            "SELF");

    private final RbacRepository repository;
    private final ObjectMapper objectMapper;
    private final RbacPermissionVersionStore versionStore;

    /**
     * 创建 RBAC 服务。
     *
     * @param repository RBAC 仓储
     * @param objectMapper JSON 序列化工具
     * @param versionStore 授权版本存储
     */
    public RbacService(
            RbacRepository repository,
            ObjectMapper objectMapper,
            RbacPermissionVersionStore versionStore) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.versionStore = versionStore;
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
     * 查询最近的授权变更记录。
     *
     * @param limit 数量上限
     * @return 审计记录
     */
    @Transactional(readOnly = true)
    public List<RbacAuditLogResponse> auditLogs(int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return repository.findAuditLogs(normalizedLimit).stream()
                .map(this::auditResponse)
                .toList();
    }

    /**
     * 创建部门。
     *
     * @param request 创建请求
     * @param operator 操作人
     * @return 部门
     */
    @Transactional
    public RbacDepartmentResponse createDepartment(
            DepartmentCreateRequest request,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        Long parentId = validateDepartmentParent(snapshot, request.parentId(), null);
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (snapshot.departments().stream().anyMatch(item -> item.code().equals(code))) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部门编码已存在");
        }
        int sortOrder = request.sortOrder() == null ? 0 : request.sortOrder();
        RbacDepartment department = repository.createDepartment(
                parentId,
                code,
                request.name().strip(),
                request.leaderUserId(),
                sortOrder);
        audit(
                operator,
                TARGET_DEPARTMENT,
                department.id(),
                "DEPARTMENT_CREATE",
                null,
                departmentSnapshot(department));
        markAuthorizationChanged();
        return departmentResponse(department, snapshot);
    }

    /**
     * 更新部门。
     *
     * @param departmentId 部门主键
     * @param request 更新请求
     * @param operator 操作人
     * @return 部门
     */
    @Transactional
    public RbacDepartmentResponse updateDepartment(
            long departmentId,
            DepartmentUpdateRequest request,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacDepartment department = requireDepartment(snapshot, departmentId);
        Long parentId = validateDepartmentParent(snapshot, request.parentId(), department.id());
        String status = request.status() == null
                ? department.status()
                : request.status().strip().toUpperCase(Locale.ROOT);
        if (!Set.of("ACTIVE", "DISABLED").contains(status)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部门状态不合法");
        }
        int sortOrder = request.sortOrder() == null
                ? department.sortOrder()
                : request.sortOrder();
        repository.updateDepartment(
                department.id(),
                parentId,
                request.name().strip(),
                request.leaderUserId(),
                status,
                sortOrder);
        RbacDepartment updated = repository.findDepartmentById(department.id())
                .orElseThrow(() -> new IllegalStateException("部门更新后未能查询到"));
        audit(
                operator,
                TARGET_DEPARTMENT,
                department.id(),
                "DEPARTMENT_UPDATE",
                departmentSnapshot(department),
                departmentSnapshot(updated));
        markAuthorizationChanged();
        return departmentResponse(updated, repository.loadSnapshot());
    }

    /**
     * 软删除部门。
     *
     * @param departmentId 部门主键
     * @param operator 操作人
     */
    @Transactional
    public void deleteDepartment(long departmentId, RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacDepartment department = requireDepartment(snapshot, departmentId);
        if (repository.countActiveDepartmentChildren(department.id()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请先删除子部门");
        }
        repository.deleteDepartment(department.id());
        audit(
                operator,
                TARGET_DEPARTMENT,
                department.id(),
                "DEPARTMENT_DELETE",
                departmentSnapshot(department),
                Map.of("status", "DELETED"));
        markAuthorizationChanged();
    }

    /**
     * 更新部门成员。
     *
     * @param departmentId 部门主键
     * @param userIds 用户主键
     * @param operator 操作人
     */
    @Transactional
    public void updateDepartmentMembers(
            long departmentId,
            List<Long> userIds,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        requireDepartment(snapshot, departmentId);
        Set<Long> normalizedIds = normalize(userIds);
        Set<Long> availableUserIds = snapshot.users().stream()
                .map(user -> user.id())
                .collect(Collectors.toSet());
        if (!availableUserIds.containsAll(normalizedIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在的用户");
        }
        for (Long userId : normalizedIds) {
            Set<Long> departmentIds = new LinkedHashSet<>(
                    snapshot.userDepartmentIds().getOrDefault(userId, List.of()));
            departmentIds.add(departmentId);
            repository.replaceUserDepartments(userId, List.copyOf(departmentIds));
        }
        snapshot.userDepartmentIds().forEach((userId, departmentIds) -> {
            if (departmentIds.contains(departmentId) && !normalizedIds.contains(userId)) {
                List<Long> remaining = departmentIds.stream()
                        .filter(id -> id != departmentId)
                        .toList();
                repository.replaceUserDepartments(userId, remaining);
            }
        });
        audit(
                operator,
                TARGET_DEPARTMENT,
                departmentId,
                "DEPARTMENT_MEMBERS_UPDATE",
                Map.of("userIds", snapshot.userDepartmentIds().entrySet().stream()
                        .filter(entry -> entry.getValue().contains(departmentId))
                        .map(Map.Entry::getKey)
                        .toList()),
                Map.of("userIds", List.copyOf(normalizedIds)));
        markAuthorizationChanged();
    }

    /**
     * 更新角色数据范围。
     *
     * @param roleId 角色主键
     * @param request 数据范围请求
     * @param operator 操作人
     */
    @Transactional
    public void updateRoleDataScope(
            long roleId,
            RoleDataScopeRequest request,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacRole role = requireMutableRole(roleId);
        String dataScope = request.dataScope().strip().toUpperCase(Locale.ROOT);
        if (!DATA_SCOPES.contains(dataScope)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "数据范围不合法");
        }
        Set<Long> departmentIds = normalize(request.departmentIds());
        Set<Long> availableDepartmentIds = snapshot.departments().stream()
                .filter(department -> !"DELETED".equals(department.status()))
                .map(department -> department.id())
                .collect(Collectors.toSet());
        if (!availableDepartmentIds.containsAll(departmentIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在的部门");
        }
        if (!"DEPT_CUSTOM".equals(dataScope)) {
            departmentIds = Set.of();
        }
        List<Long> beforeDepartments =
                snapshot.roleDataScopeDepartmentIds().getOrDefault(roleId, List.of());
        repository.replaceRoleDataScope(roleId, dataScope, List.copyOf(departmentIds));
        audit(
                operator,
                TARGET_ROLE,
                roleId,
                "ROLE_DATA_SCOPE_UPDATE",
                Map.of(
                        "dataScope", role.dataScope(),
                        "departmentIds", beforeDepartments),
                Map.of(
                        "dataScope", dataScope,
                        "departmentIds", List.copyOf(departmentIds)));
        markAuthorizationChanged();
    }

    /**
     * 更新用户所属部门。
     *
     * @param userId 用户主键
     * @param departmentIds 部门主键
     * @param operator 操作人
     */
    @Transactional
    public void updateUserDepartments(
            long userId,
            List<Long> departmentIds,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        if (snapshot.users().stream().noneMatch(user -> user.id() == userId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        Set<Long> normalizedIds = normalize(departmentIds);
        Set<Long> availableDepartmentIds = snapshot.departments().stream()
                .filter(department -> "ACTIVE".equals(department.status()))
                .map(department -> department.id())
                .collect(Collectors.toSet());
        if (!availableDepartmentIds.containsAll(normalizedIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在或已停用的部门");
        }
        List<Long> beforeIds = snapshot.userDepartmentIds().getOrDefault(userId, List.of());
        repository.replaceUserDepartments(userId, List.copyOf(normalizedIds));
        audit(
                operator,
                TARGET_USER,
                userId,
                "USER_DEPARTMENTS_UPDATE",
                Map.of("departmentIds", beforeIds),
                Map.of("departmentIds", List.copyOf(normalizedIds)));
        markAuthorizationChanged();
    }

    /**
     * 创建自定义权限点，并自动授予超级管理员。
     *
     * @param request 创建请求
     * @return 创建的权限点
     */
    @Transactional
    public RbacPermissionResponse createPermission(
            PermissionCreateRequest request,
            RbacOperator operator) {
        String code = request.code().strip().toLowerCase(Locale.ROOT);
        RbacPermission parent = request.parentId() == null
                ? null
                : requirePermissionParent(request.parentId());
        String module = parent == null ? request.module().strip() : parent.module();
        String type = request.type().strip().toUpperCase(Locale.ROOT);
        String riskLevel = request.riskLevel() == null
                ? "LOW"
                : request.riskLevel().strip().toUpperCase(Locale.ROOT);
        RbacPermission permission;
        try {
            permission = repository.createPermission(
                    code,
                    request.name().strip(),
                    module,
                    type,
                    request.parentId(),
                    nullIfBlank(request.routePath()),
                    riskLevel,
                    nullIfBlank(request.description()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.PERMISSION_CODE_EXISTS);
        }
        RbacRole superAdminRole = repository.findRoleByCode(ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("缺少超级管理员角色"));
        repository.assignPermissionToRole(superAdminRole.id(), permission.id());
        audit(
                operator,
                TARGET_PERMISSION,
                permission.id(),
                "PERMISSION_CREATE",
                null,
                permissionSnapshot(permission));
        markAuthorizationChanged();
        log.info("RBAC permission created permission={}", code);
        return permissionResponse(permission);
    }

    /**
     * 删除自定义权限点及其角色关联。
     *
     * @param permissionId 权限主键
     */
    @Transactional
    public void deletePermission(long permissionId, RbacOperator operator) {
        RbacPermission permission = repository.findPermissionById(permissionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "权限不存在"));
        if (permission.builtIn()) {
            throw new BusinessException(ErrorCode.PERMISSION_PROTECTED);
        }
        if (repository.countActivePermissionChildren(permission.id()) > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请先删除该权限下的子权限");
        }
        repository.deletePermission(permission.id());
        audit(
                operator,
                TARGET_PERMISSION,
                permission.id(),
                "PERMISSION_DELETE",
                permissionSnapshot(permission),
                Map.of("status", "DELETED"));
        markAuthorizationChanged();
        log.info("RBAC permission deleted permission={}", permission.code());
    }

    /**
     * 创建自定义角色。
     *
     * @param request 创建请求
     * @return 创建的角色
     */
    @Transactional
    public RbacRoleResponse createRole(RoleCreateRequest request, RbacOperator operator) {
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (repository.findRoleByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        RbacRole role;
        try {
            role = repository.createRole(
                    code,
                    request.name().strip(),
                    nullIfBlank(request.description()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        RbacSnapshot snapshot = repository.loadSnapshot();
        Set<Long> permissionIds = expandPermissionDependencies(
                snapshot,
                normalize(request.permissionIds()));
        validatePermissionIds(snapshot, permissionIds);
        repository.replaceRolePermissions(role.id(), List.copyOf(permissionIds));
        audit(
                operator,
                TARGET_ROLE,
                role.id(),
                "ROLE_CREATE",
                null,
                roleSnapshot(role, permissionIds));
        markAuthorizationChanged();
        log.info("RBAC role created role={} permissions={}", code, permissionIds);
        return roleResponse(repository.loadSnapshot(), role.id());
    }

    /**
     * 复制角色资料、权限和成员。
     *
     * @param sourceRoleId 源角色主键
     * @param request 复制请求
     * @param operator 操作人
     * @return 新角色
     */
    @Transactional
    public RbacRoleResponse copyRole(
            long sourceRoleId,
            RoleCopyRequest request,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacRole sourceRole = snapshot.roles().stream()
                .filter(role -> role.id() == sourceRoleId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "源角色不存在"));
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (repository.findRoleByCode(code).isPresent()) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }
        RbacRole copiedRole;
        try {
            copiedRole = repository.createRole(
                    code,
                    request.name().strip(),
                    nullIfBlank(request.description()));
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.ROLE_CODE_EXISTS);
        }

        Set<Long> permissionIds = Set.of();
        if (Boolean.TRUE.equals(request.copyPermissions())) {
            Set<Long> sourcePermissionIds = new LinkedHashSet<>(
                    snapshot.rolePermissionIds().getOrDefault(sourceRoleId, List.of()));
            Set<Long> assignablePermissionIds = snapshot.permissions().stream()
                    .filter(permission -> sourcePermissionIds.contains(permission.id()))
                    .filter(permission -> "ACTIVE".equals(permission.status()))
                    .filter(permission -> !"CATALOG".equals(permission.type()))
                    .map(RbacPermission::id)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            permissionIds = expandPermissionDependencies(
                    snapshot,
                    assignablePermissionIds);
        }
        repository.replaceRolePermissions(copiedRole.id(), List.copyOf(permissionIds));

        List<Long> copiedUserIds = List.of();
        if (Boolean.TRUE.equals(request.copyMembers())) {
            copiedUserIds = repository.findUserIdsByRole(sourceRoleId);
            repository.assignUsersToRole(copiedRole.id(), copiedUserIds);
        }
        Map<String, Object> after = roleSnapshot(copiedRole, permissionIds);
        after.put("copyMembers", Boolean.TRUE.equals(request.copyMembers()));
        after.put("memberIds", copiedUserIds);
        audit(
                operator,
                TARGET_ROLE,
                copiedRole.id(),
                "ROLE_COPY",
                Map.of("sourceRoleId", sourceRole.id()),
                after);
        markAuthorizationChanged();
        log.info(
                "RBAC role copied source={} target={} permissions={} members={}",
                sourceRole.code(),
                copiedRole.code(),
                permissionIds,
                copiedUserIds.size());
        return roleResponse(repository.loadSnapshot(), copiedRole.id());
    }

    /**
     * 更新自定义角色名称和说明。
     *
     * @param roleId 角色主键
     * @param request 更新请求
     * @return 更新后的角色
     */
    @Transactional
    public RbacRoleResponse updateRole(
            long roleId,
            RoleUpdateRequest request,
            RbacOperator operator) {
        RbacRole role = requireMutableRole(roleId);
        Map<String, Object> before = roleSnapshot(
                role,
                repository.loadSnapshot().rolePermissionIds().getOrDefault(role.id(), List.of()));
        repository.updateRole(
                role.id(),
                request.name().strip(),
                nullIfBlank(request.description()));
        RbacSnapshot updatedSnapshot = repository.loadSnapshot();
        RbacRole updated = repository.findRoleById(role.id())
                .orElseThrow(() -> new IllegalStateException("角色更新后未能查询到"));
        audit(
                operator,
                TARGET_ROLE,
                role.id(),
                "ROLE_UPDATE",
                before,
                roleSnapshot(
                        updated,
                        updatedSnapshot.rolePermissionIds().getOrDefault(updated.id(), List.of())));
        markAuthorizationChanged();
        log.info("RBAC role updated role={}", role.code());
        return roleResponse(repository.loadSnapshot(), role.id());
    }

    /**
     * 启用或停用自定义角色。
     *
     * @param roleId 角色主键
     * @param status ACTIVE 或 DISABLED
     * @return 更新后的角色
     */
    @Transactional
    public RbacRoleResponse updateRoleStatus(
            long roleId,
            String status,
            RbacOperator operator) {
        RbacRole role = requireMutableRole(roleId);
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ErrorCode.ROLE_STATE_INVALID);
        }
        repository.updateRoleStatus(role.id(), status);
        audit(
                operator,
                TARGET_ROLE,
                role.id(),
                "ROLE_STATUS_UPDATE",
                Map.of("status", role.status()),
                Map.of("status", status));
        markAuthorizationChanged();
        log.info("RBAC role status updated role={} status={}", role.code(), status);
        return roleResponse(repository.loadSnapshot(), role.id());
    }

    /**
     * 软删除自定义角色并清理授权关系。
     *
     * @param roleId 角色主键
     */
    @Transactional
    public void deleteRole(long roleId, RbacOperator operator) {
        RbacRole role = requireMutableRole(roleId);
        Map<String, Object> before = roleSnapshot(
                role,
                repository.loadSnapshot().rolePermissionIds().getOrDefault(role.id(), List.of()));
        repository.deleteRole(role.id());
        audit(
                operator,
                TARGET_ROLE,
                role.id(),
                "ROLE_DELETE",
                before,
                Map.of("status", "DELETED"));
        markAuthorizationChanged();
        log.info("RBAC role deleted role={}", role.code());
    }

    /**
     * 更新角色权限。
     *
     * @param roleId 角色主键
     * @param permissionIds 权限主键
     */
    @Transactional
    public void updateRolePermissions(
            long roleId,
            List<Long> permissionIds,
            boolean confirmHighRisk,
            RbacOperator operator) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        RbacRole role = findRole(snapshot.roles(), roleId);
        if (ROLE_SUPER_ADMIN.equals(role.code())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "超级管理员权限固定为全部权限");
        }
        Set<Long> normalizedIds = expandPermissionDependencies(
                snapshot,
                normalize(permissionIds));
        validatePermissionIds(snapshot, normalizedIds);
        List<Long> beforeIds = snapshot.rolePermissionIds().getOrDefault(roleId, List.of());
        requireHighRiskConfirmation(snapshot, beforeIds, normalizedIds, confirmHighRisk);
        repository.replaceRolePermissions(roleId, List.copyOf(normalizedIds));
        audit(
                operator,
                TARGET_ROLE,
                roleId,
                "ROLE_PERMISSIONS_UPDATE",
                Map.of("permissionIds", beforeIds),
                Map.of("permissionIds", List.copyOf(normalizedIds)));
        markAuthorizationChanged();
        log.info("RBAC role permission updated role={} permissions={}", role.code(), normalizedIds);
    }

    /**
     * 更新用户角色。
     *
     * @param userId 用户主键
     * @param roleIds 角色主键
     */
    @Transactional
    public void updateUserRoles(
            long userId,
            List<Long> roleIds,
            RbacOperator operator) {
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
        List<Long> beforeIds = List.copyOf(currentRoleIds);
        repository.replaceUserRoles(userId, List.copyOf(normalizedIds));
        audit(
                operator,
                TARGET_USER,
                userId,
                "USER_ROLES_UPDATE",
                Map.of("roleIds", beforeIds),
                Map.of("roleIds", List.copyOf(normalizedIds)));
        markAuthorizationChanged();
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
                        role.status(),
                        userCounts.getOrDefault(role.id(), 0),
                        snapshot.rolePermissionIds().getOrDefault(role.id(), List.of()),
                        role.dataScope(),
                        snapshot.roleDataScopeDepartmentIds()
                                .getOrDefault(role.id(), List.of())))
                .toList();

        Map<String, List<RbacPermissionResponse>> grouped = new LinkedHashMap<>();
        for (RbacPermission permission : snapshot.permissions()) {
            grouped.computeIfAbsent(permission.module(), key -> new java.util.ArrayList<>())
                    .add(new RbacPermissionResponse(
                            permission.id(),
                            permission.parentId(),
                            permission.code(),
                            permission.name(),
                            permission.module(),
                            permission.type(),
                            permission.routePath(),
                            permission.description(),
                            permission.status(),
                            permission.riskLevel(),
                            permission.builtIn()));
        }
        List<RbacPermissionGroupResponse> permissionGroups = grouped.entrySet().stream()
                .map(entry -> new RbacPermissionGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
        List<RbacPermissionPackageResponse> permissionPackages =
                snapshot.permissionPackages().stream()
                        .map(permissionPackage -> new RbacPermissionPackageResponse(
                                permissionPackage.id(),
                                permissionPackage.code(),
                                permissionPackage.name(),
                                permissionPackage.description(),
                                snapshot.packagePermissionIds()
                                        .getOrDefault(permissionPackage.id(), List.of())))
                        .toList();
        List<RbacPermissionDependencyResponse> permissionDependencies =
                snapshot.permissionDependencies().stream()
                        .map(dependency -> new RbacPermissionDependencyResponse(
                                dependency.permissionId(),
                                dependency.requiredPermissionId()))
                        .toList();
        List<RbacDepartmentResponse> departments = snapshot.departments().stream()
                .map(department -> departmentResponse(department, snapshot))
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
                            assignedRoleCodes,
                            snapshot.userDepartmentIds().getOrDefault(user.id(), List.of()));
                })
                .toList();
        return new RbacOverviewResponse(
                roles,
                permissionGroups,
                permissionPackages,
                permissionDependencies,
                departments,
                users);
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
        return ids == null ? Set.of() : new LinkedHashSet<>(ids);
    }

    private void validatePermissionIds(RbacSnapshot snapshot, Set<Long> permissionIds) {
        Set<Long> availablePermissionIds = snapshot.permissions().stream()
                .filter(permission -> "ACTIVE".equals(permission.status()))
                .filter(permission -> !"CATALOG".equals(permission.type()))
                .map(RbacPermission::id)
                .collect(Collectors.toSet());
        if (!availablePermissionIds.containsAll(permissionIds)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "包含不存在的权限");
        }
        Long rbacManageId = snapshot.permissions().stream()
                .filter(permission -> PERMISSION_RBAC_MANAGE.equals(permission.code()))
                .map(RbacPermission::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少权限管理权限点"));
        if (permissionIds.contains(rbacManageId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "权限管理只能由超级管理员角色拥有");
        }
    }

    private Set<Long> expandPermissionDependencies(
            RbacSnapshot snapshot,
            Set<Long> permissionIds) {
        Set<Long> expanded = new LinkedHashSet<>(permissionIds);
        Map<Long, List<Long>> dependencies = snapshot.permissionDependencies().stream()
                .collect(Collectors.groupingBy(
                        dependency -> dependency.permissionId(),
                        Collectors.mapping(
                                dependency -> dependency.requiredPermissionId(),
                                Collectors.toList())));
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Long> current = List.copyOf(expanded);
            for (Long permissionId : current) {
                for (Long requiredId : dependencies.getOrDefault(permissionId, List.of())) {
                    if (expanded.add(requiredId)) {
                        changed = true;
                    }
                }
            }
        }
        return expanded;
    }

    private void requireHighRiskConfirmation(
            RbacSnapshot snapshot,
            List<Long> beforeIds,
            Set<Long> afterIds,
            boolean confirmHighRisk) {
        if (confirmHighRisk) {
            return;
        }
        Set<Long> beforeSet = Set.copyOf(beforeIds);
        boolean addsHighRisk = snapshot.permissions().stream()
                .anyMatch(permission -> afterIds.contains(permission.id())
                        && !beforeSet.contains(permission.id())
                        && "HIGH".equals(permission.riskLevel()));
        if (addsHighRisk) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "包含新增高风险权限，请确认后保存");
        }
    }

    private RbacRole requireMutableRole(long roleId) {
        RbacRole role = repository.findRoleById(roleId)
                .filter(candidate -> !"DELETED".equals(candidate.status()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在"));
        if (role.builtIn()) {
            throw new BusinessException(ErrorCode.ROLE_PROTECTED);
        }
        return role;
    }

    private RbacRoleResponse roleResponse(RbacSnapshot snapshot, long roleId) {
        RbacRole role = findRole(snapshot.roles(), roleId);
        int userCount = snapshot.userRoleIds().values().stream()
                .mapToInt(roleIds -> roleIds.contains(roleId) ? 1 : 0)
                .sum();
        return new RbacRoleResponse(
                role.id(),
                role.code(),
                role.name(),
                role.description(),
                role.builtIn(),
                role.status(),
                userCount,
                snapshot.rolePermissionIds().getOrDefault(roleId, List.of()),
                role.dataScope(),
                snapshot.roleDataScopeDepartmentIds().getOrDefault(roleId, List.of()));
    }

    private RbacPermissionResponse permissionResponse(RbacPermission permission) {
        return new RbacPermissionResponse(
                permission.id(),
                permission.parentId(),
                permission.code(),
                permission.name(),
                permission.module(),
                permission.type(),
                permission.routePath(),
                permission.description(),
                permission.status(),
                permission.riskLevel(),
                permission.builtIn());
    }

    private RbacPermission requirePermissionParent(long parentId) {
        RbacPermission parent = repository.findPermissionById(parentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_INVALID, "父权限不存在"));
        if (!"ACTIVE".equals(parent.status())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "父权限未启用");
        }
        if (!"CATALOG".equals(parent.type()) && !"MENU".equals(parent.type())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "父权限必须是目录或菜单");
        }
        return parent;
    }

    private void audit(
            RbacOperator operator,
            String targetType,
            Long targetId,
            String action,
            Object before,
            Object after) {
        repository.appendAuditLog(new RbacAuditLog(
                0,
                operator.id(),
                operator.username(),
                targetType,
                targetId,
                action,
                before == null ? null : toJson(before),
                after == null ? null : toJson(after),
                operator.clientIp(),
                null));
    }

    private RbacAuditLogResponse auditResponse(RbacAuditLog log) {
        return new RbacAuditLogResponse(
                log.id(),
                log.operatorId(),
                log.operatorName(),
                log.targetType(),
                log.targetId(),
                log.action(),
                log.beforeValue(),
                log.afterValue(),
                log.clientIp(),
                log.createTime());
    }

    private Map<String, Object> permissionSnapshot(RbacPermission permission) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", permission.id());
        snapshot.put("parentId", permission.parentId());
        snapshot.put("code", permission.code());
        snapshot.put("name", permission.name());
        snapshot.put("module", permission.module());
        snapshot.put("type", permission.type());
        snapshot.put("routePath", permission.routePath());
        snapshot.put("status", permission.status());
        snapshot.put("riskLevel", permission.riskLevel());
        snapshot.put("builtIn", permission.builtIn());
        return snapshot;
    }

    private Map<String, Object> roleSnapshot(RbacRole role, java.util.Collection<Long> permissionIds) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", role.id());
        snapshot.put("code", role.code());
        snapshot.put("name", role.name());
        snapshot.put("description", role.description());
        snapshot.put("status", role.status());
        snapshot.put("dataScope", role.dataScope());
        snapshot.put("permissionIds", List.copyOf(permissionIds));
        return snapshot;
    }

    private RbacDepartment requireDepartment(RbacSnapshot snapshot, long departmentId) {
        return snapshot.departments().stream()
                .filter(department -> department.id() == departmentId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "部门不存在"));
    }

    private Long validateDepartmentParent(
            RbacSnapshot snapshot,
            Long parentId,
            Long departmentId) {
        if (parentId == null) {
            return null;
        }
        RbacDepartment parent = requireDepartment(snapshot, parentId);
        if ("DELETED".equals(parent.status())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "父部门不存在");
        }
        if (departmentId == null) {
            return parent.id();
        }
        if (departmentId.equals(parent.id())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "部门不能作为自己的上级");
        }
        Long currentParentId = parent.parentId();
        Set<Long> visited = new LinkedHashSet<>();
        while (currentParentId != null && visited.add(currentParentId)) {
            if (currentParentId.equals(departmentId)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "不能形成部门循环层级");
            }
            RbacDepartment current = requireDepartment(snapshot, currentParentId);
            currentParentId = current.parentId();
        }
        return parent.id();
    }

    private RbacDepartmentResponse departmentResponse(
            RbacDepartment department,
            RbacSnapshot snapshot) {
        List<Long> memberIds = snapshot.userDepartmentIds().entrySet().stream()
                .filter(entry -> entry.getValue().contains(department.id()))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        return new RbacDepartmentResponse(
                department.id(),
                department.parentId(),
                department.code(),
                department.name(),
                department.leaderUserId(),
                department.status(),
                department.sortOrder(),
                memberIds);
    }

    private Map<String, Object> departmentSnapshot(RbacDepartment department) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", department.id());
        snapshot.put("parentId", department.parentId());
        snapshot.put("code", department.code());
        snapshot.put("name", department.name());
        snapshot.put("leaderUserId", department.leaderUserId());
        snapshot.put("status", department.status());
        snapshot.put("sortOrder", department.sortOrder());
        return snapshot;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("授权审计内容序列化失败", exception);
        }
    }

    private void markAuthorizationChanged() {
        versionStore.bumpVersion();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                versionStore.bumpVersion();
            }
        });
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
