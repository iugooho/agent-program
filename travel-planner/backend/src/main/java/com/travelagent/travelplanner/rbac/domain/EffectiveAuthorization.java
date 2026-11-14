package com.travelagent.travelplanner.rbac.domain;

import java.util.List;

/**
 * 用户当前生效的角色和权限。
 *
 * @param roles 角色编码
 * @param permissions 权限编码
 */
public record EffectiveAuthorization(List<String> roles, List<String> permissions) {

    /** 空授权，用于用户尚未分配角色时保持登录态但不授予业务权限。 */
    public static EffectiveAuthorization empty() {
        return new EffectiveAuthorization(List.of(), List.of());
    }

    /** 是否拥有指定角色。 */
    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }
}
