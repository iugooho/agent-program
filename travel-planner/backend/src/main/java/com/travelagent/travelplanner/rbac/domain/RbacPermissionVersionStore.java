package com.travelagent.travelplanner.rbac.domain;

/** RBAC 授权版本存储。 */
public interface RbacPermissionVersionStore {

    /**
     * 读取当前授权版本。
     *
     * @return 版本号
     */
    long currentVersion();

    /**
     * 递增授权版本。
     *
     * @return 新版本号
     */
    long bumpVersion();
}
