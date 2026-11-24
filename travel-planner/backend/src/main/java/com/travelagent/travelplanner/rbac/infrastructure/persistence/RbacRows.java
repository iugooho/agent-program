package com.travelagent.travelplanner.rbac.infrastructure.persistence;

import java.time.LocalDateTime;

/** RBAC Mapper 查询行。 */
public final class RbacRows {

    private RbacRows() {
    }

    /**
     * 角色行。
     *
     * @param id 角色主键
     * @param code 角色编码
     * @param name 角色名称
     * @param description 角色说明
     * @param builtIn 是否内置
 * @param status 角色状态
     * @param dataScope 数据范围
     */
    public record RoleRow(
            long id,
            String code,
            String name,
            String description,
            boolean builtIn,
            String status,
            String dataScope) {
    }

    /**
     * 权限行。
     *
     * @param id 权限主键
     * @param parentId 父权限主键
     * @param code 权限编码
     * @param name 权限名称
     * @param module 权限分组
     * @param type 权限类型
     * @param routePath 前端路由
     * @param description 权限说明
     * @param status 权限状态
     * @param riskLevel 风险等级
     * @param builtIn 是否内置权限
     */
    public record PermissionRow(
            long id,
            Long parentId,
            String code,
            String name,
            String module,
            String type,
            String routePath,
            String description,
            String status,
            String riskLevel,
            boolean builtIn) {
    }

    /**
     * 权限包行。
     *
     * @param id 主键
     * @param code 编码
     * @param name 名称
     * @param description 说明
     * @param status 状态
     */
    public record PermissionPackageRow(
            long id,
            String code,
            String name,
            String description,
            String status) {
    }

    /**
     * 权限依赖行。
     *
     * @param permissionId 权限主键
     * @param requiredPermissionId 前置权限主键
     */
    public record PermissionDependencyRow(
            long permissionId,
            long requiredPermissionId) {
    }

    /**
     * 用户行。
     *
     * @param id 用户主键
     * @param username 登录账号
     * @param nickname 昵称
     * @param email 邮箱
     * @param status 状态
     */
    public record UserRow(long id, String username, String nickname, String email, String status) {
    }

    /**
     * 部门行。
     *
     * @param id 主键
     * @param parentId 父部门主键
     * @param code 部门编码
     * @param name 部门名称
     * @param leaderUserId 负责人主键
     * @param status 状态
     * @param sortOrder 排序值
     */
    public record DepartmentRow(
            long id,
            Long parentId,
            String code,
            String name,
            Long leaderUserId,
            String status,
            int sortOrder) {
    }

    /**
     * 关系行。
     *
     * @param ownerId 用户或角色主键
     * @param targetId 角色或权限主键
     */
    public record AssignmentRow(long ownerId, long targetId) {
    }

    /**
     * 授权审计记录行。
     *
     * @param id 主键
     * @param operatorId 操作人主键
     * @param operatorName 操作人账号
     * @param targetType 目标类型
     * @param targetId 目标主键
     * @param action 操作动作
     * @param beforeValue 变更前内容
     * @param afterValue 变更后内容
     * @param clientIp 客户端 IP
     * @param createTime 操作时间
     */
    public record AuditLogRow(
            long id,
            Long operatorId,
            String operatorName,
            String targetType,
            Long targetId,
            String action,
            String beforeValue,
            String afterValue,
            String clientIp,
            LocalDateTime createTime) {
    }
}
