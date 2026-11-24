package com.travelagent.travelplanner.rbac.application;

import com.travelagent.travelplanner.rbac.domain.RbacDataScope;
import com.travelagent.travelplanner.rbac.domain.RbacDepartment;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import com.travelagent.travelplanner.rbac.domain.RbacRole;
import com.travelagent.travelplanner.rbac.domain.RbacSnapshot;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 计算用户生效数据范围。 */
@Service
public class RbacDataScopeService {

    private final RbacRepository repository;

    /**
     * 创建数据范围服务。
     *
     * @param repository RBAC 仓储
     */
    public RbacDataScopeService(RbacRepository repository) {
        this.repository = repository;
    }

    /**
     * 计算用户生效数据范围。
     *
     * @param userId 用户主键
     * @return 数据范围
     */
    public RbacDataScope resolve(long userId) {
        RbacSnapshot snapshot = repository.loadSnapshot();
        Set<Long> userDepartmentIds = new LinkedHashSet<>(
                snapshot.userDepartmentIds().getOrDefault(userId, List.of()));
        Set<Long> allowedDepartmentIds = new LinkedHashSet<>();
        boolean allData = false;
        boolean selfAllowed = false;
        Map<Long, RbacRole> roleById = snapshot.roles().stream()
                .collect(Collectors.toMap(RbacRole::id, role -> role));
        for (Long roleId : snapshot.userRoleIds().getOrDefault(userId, List.of())) {
            RbacRole role = roleById.get(roleId);
            if (role == null || !"ACTIVE".equals(role.status())) {
                continue;
            }
            switch (role.dataScope()) {
                case "ALL" -> allData = true;
                case "SELF" -> selfAllowed = true;
                case "DEPT_ONLY" -> allowedDepartmentIds.addAll(userDepartmentIds);
                case "DEPT_AND_CHILD" -> userDepartmentIds.forEach(departmentId ->
                        allowedDepartmentIds.addAll(
                                departmentAndDescendants(snapshot.departments(), departmentId)));
                case "DEPT_CUSTOM" -> allowedDepartmentIds.addAll(
                        snapshot.roleDataScopeDepartmentIds().getOrDefault(roleId, List.of()));
                default -> {
                    // 未知范围不授予数据权限。
                }
            }
        }
        if (allData) {
            return new RbacDataScope(true, true, Set.of());
        }
        return new RbacDataScope(false, selfAllowed, allowedDepartmentIds);
    }

    /**
     * 判断用户是否可访问资源。
     *
     * @param userId 用户主键
     * @param ownerId 资源所有者主键
     * @param departmentId 资源所属部门主键
     * @return true 表示可访问
     */
    public boolean canAccess(long userId, long ownerId, Long departmentId) {
        RbacDataScope scope = resolve(userId);
        if (scope.allData()) {
            return true;
        }
        if (scope.selfAllowed() && ownerId == userId) {
            return true;
        }
        return departmentId != null && scope.departmentIds().contains(departmentId);
    }

    /**
     * 获取用户用于新资源归属的主部门。
     *
     * @param userId 用户主键
     * @return 部门主键，没有部门时返回 null
     */
    public Long primaryDepartmentId(long userId) {
        return repository.loadSnapshot().userDepartmentIds().getOrDefault(userId, List.of())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Set<Long> departmentAndDescendants(
            List<RbacDepartment> departments,
            long departmentId) {
        Set<Long> result = new LinkedHashSet<>();
        collectDepartmentAndChildren(departments, departmentId, result);
        return result;
    }

    private void collectDepartmentAndChildren(
            List<RbacDepartment> departments,
            long departmentId,
            Set<Long> result) {
        if (!result.add(departmentId)) {
            return;
        }
        departments.stream()
                .filter(department -> department.parentId() != null
                        && department.parentId() == departmentId)
                .forEach(department ->
                        collectDepartmentAndChildren(departments, department.id(), result));
    }
}
