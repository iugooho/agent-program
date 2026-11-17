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
     * 创建角色。
     *
     * @param code 角色编码
     * @param name 角色名称
     * @param description 角色说明
     * @return 创建的角色
     */
    RbacRole createRole(String code, String name, String description);

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
}
