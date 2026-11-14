package com.travelagent.travelplanner.rbac.domain;

/**
 * 权限分配页面使用的用户摘要。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param nickname 昵称
 * @param email 邮箱
 * @param status 账号状态
 */
public record RbacUser(long id, String username, String nickname, String email, String status) {
}
