package com.travelagent.travelplanner.rbac.infrastructure.persistence;

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
     */
    public record RoleRow(long id, String code, String name, String description, boolean builtIn) {
    }

    /**
     * 权限行。
     *
     * @param id 权限主键
     * @param code 权限编码
     * @param name 权限名称
     * @param module 权限分组
     * @param description 权限说明
     */
    public record PermissionRow(
            long id,
            String code,
            String name,
            String module,
            String description) {
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
     * 关系行。
     *
     * @param ownerId 用户或角色主键
     * @param targetId 角色或权限主键
     */
    public record AssignmentRow(long ownerId, long targetId) {
    }
}
