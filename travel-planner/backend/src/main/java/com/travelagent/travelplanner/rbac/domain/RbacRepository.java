package com.travelagent.travelplanner.rbac.domain;

import java.util.List;

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
