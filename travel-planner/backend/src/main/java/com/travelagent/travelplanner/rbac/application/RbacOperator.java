package com.travelagent.travelplanner.rbac.application;

/**
 * RBAC 操作人上下文。
 *
 * @param id 用户主键
 * @param username 登录账号
 * @param clientIp 客户端 IP
 */
public record RbacOperator(long id, String username, String clientIp) {
}
