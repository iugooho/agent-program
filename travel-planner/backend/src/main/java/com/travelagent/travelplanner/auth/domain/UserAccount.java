package com.travelagent.travelplanner.auth.domain;

import java.time.LocalDateTime;

/**
 * 登录用户模型。
 *
 * @param id 用户主键
 * @param username 独立登录账号
 * @param email 登录邮箱
 * @param passwordHash BCrypt 密码哈希
 * @param role USER 或 ADMIN
 * @param status ACTIVE、DISABLED 或 DELETED
 * @param nickname 昵称
 * @param phone 联系电话
 * @param createTime 创建时间
 * @param lastLoginTime 最近登录时间
 */
public record UserAccount(
        long id,
        String username,
        String email,
        String passwordHash,
        String role,
        String status,
        String nickname,
        String phone,
        LocalDateTime createTime,
        LocalDateTime lastLoginTime) {

    /** 用户状态是否允许登录。 */
    public boolean active() {
        return "ACTIVE".equals(status);
    }

    /** 用户是否已被管理员禁用。 */
    public boolean disabled() {
        return "DISABLED".equals(status);
    }
}
