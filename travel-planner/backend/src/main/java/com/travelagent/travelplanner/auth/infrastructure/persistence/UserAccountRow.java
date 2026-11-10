package com.travelagent.travelplanner.auth.infrastructure.persistence;

import java.time.LocalDateTime;

/**
 * users 表持久化行。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param email 登录邮箱
 * @param passwordHash 密码哈希
 * @param role 角色
 * @param status 状态
 * @param nickname 昵称
 * @param phone 联系电话
 * @param createTime 创建时间
 * @param lastLoginTime 最近登录时间
 */
public record UserAccountRow(
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
}
