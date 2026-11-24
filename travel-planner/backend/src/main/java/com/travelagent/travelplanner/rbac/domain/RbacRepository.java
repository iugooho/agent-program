package com.travelagent.travelplanner.rbac.domain;

import java.util.List;
import java.util.Optional;

/** RBAC 仓储接口。 */
public interface RbacRepository {

    /**
     * 查询用户当前生效的授权。
     *
     * @param userId 用户主键
     * @return 当前授权
     */
    EffectiveAuthorization findEffectiveAuthorization(long userId);

    /**
     * 读取权限配置页面快照。
     *
     * @return 当前角色、权限和用户关系
     */
    RbacSnapshot loadSnapshot();

    /**
     * 按主键查询角色。
     *
     * @param roleId 角色主键
     * @return 角色
     */
    Optional<RbacRole> findRoleById(long roleId);

    /**
     * 按编码查询角色。
     *
     * @param code 角色编码
     * @return 角色
     */
    Optional<RbacRole> findRoleByCode(String code);

    /**
     * 按主键查询权限点。
     *
     * @param permissionId 权限主键
     * @return 权限点
     */
    Optional<RbacPermission> findPermissionById(long permissionId);

    /**
     * 按主键查询部门。
     *
     * @param departmentId 部门主键
     * @return 部门
     */
    Optional<RbacDepartment> findDepartmentById(long departmentId);

    /**
     * 创建部门。
     *
     * @param parentId 父部门主键
     * @param code 部门编码
     * @param name 部门名称
     * @param leaderUserId 负责人主键
     * @param sortOrder 排序值
     * @return 创建后的部门
     */
    RbacDepartment createDepartment(
            Long parentId,
            String code,
            String name,
            Long leaderUserId,
            int sortOrder);

    /**
     * 更新部门。
     *
     * @param departmentId 部门主键
     * @param parentId 父部门主键
     * @param name 部门名称
     * @param leaderUserId 负责人主键
     * @param status 状态
     * @param sortOrder 排序值
     */
    void updateDepartment(
            long departmentId,
            Long parentId,
            String name,
            Long leaderUserId,
            String status,
            int sortOrder);

    /**
     * 软删除部门。
     *
     * @param departmentId 部门主键
     */
    void deleteDepartment(long departmentId);

    /**
     * 查询有效子部门数量。
     *
     * @param departmentId 部门主键
     * @return 子部门数量
     */
    int countActiveDepartmentChildren(long departmentId);

    /**
     * 创建角色。
     *
     * @param code 角色编码
     * @param name 角色名称
     * @param description 角色说明
     * @return 创建的角色
     */
    RbacRole createRole(String code, String name, String description);

    /**
     * 创建自定义权限点。
     *
     * @param code 权限编码
     * @param name 权限名称
     * @param module 权限分组
     * @param type 权限类型
     * @param parentId 父权限主键
     * @param routePath 前端路由
     * @param riskLevel 风险等级
     * @param description 权限说明
     * @return 创建的权限点
     */
    RbacPermission createPermission(
            String code,
            String name,
            String module,
            String type,
            Long parentId,
            String routePath,
            String riskLevel,
            String description);

    /**
     * 更新角色名称和说明。
     *
     * @param roleId 角色主键
     * @param name 角色名称
     * @param description 角色说明
     */
    void updateRole(long roleId, String name, String description);

    /**
     * 更新角色状态。
     *
     * @param roleId 角色主键
     * @param status 目标状态
     */
    void updateRoleStatus(long roleId, String status);

    /**
     * 软删除角色并清理授权关系。
     *
     * @param roleId 角色主键
     */
    void deleteRole(long roleId);

    /**
     * 删除自定义权限点及其角色关联。
     *
     * @param permissionId 权限主键
     */
    void deletePermission(long permissionId);

    /**
     * 查询权限下仍有效的直接子权限数量。
     *
     * @param permissionId 权限主键
     * @return 子权限数量
     */
    int countActivePermissionChildren(long permissionId);

    /**
     * 给角色批量添加成员。
     *
     * @param roleId 角色主键
     * @param userIds 用户主键
     */
    void assignUsersToRole(long roleId, List<Long> userIds);

    /**
     * 查询角色下的用户主键。
     *
     * @param roleId 角色主键
     * @return 用户主键
     */
    List<Long> findUserIdsByRole(long roleId);

    /**
     * 给角色新增一个权限点。
     *
     * @param roleId 角色主键
     * @param permissionId 权限主键
     */
    void assignPermissionToRole(long roleId, long permissionId);

    /**
     * 替换角色权限。
     *
     * @param roleId 角色主键
     * @param permissionIds 权限主键
     */
    void replaceRolePermissions(long roleId, List<Long> permissionIds);

    /**
     * 替换用户角色。
     *
     * @param userId 用户主键
     * @param roleIds 角色主键
     */
    void replaceUserRoles(long userId, List<Long> roleIds);

    /**
     * 写入授权审计记录。
     *
     * @param auditLog 审计记录
     */
    void appendAuditLog(RbacAuditLog auditLog);

    /**
     * 查询最近的授权审计记录。
     *
     * @param limit 数量上限
     * @return 审计记录
     */
    List<RbacAuditLog> findAuditLogs(int limit);

    /**
     * 替换用户所属部门。
     *
     * @param userId 用户主键
     * @param departmentIds 部门主键
     */
    void replaceUserDepartments(long userId, List<Long> departmentIds);

    /**
     * 替换角色数据范围和自定义部门。
     *
     * @param roleId 角色主键
     * @param dataScope 数据范围
     * @param departmentIds 自定义部门主键
     */
    void replaceRoleDataScope(long roleId, String dataScope, List<Long> departmentIds);
}
