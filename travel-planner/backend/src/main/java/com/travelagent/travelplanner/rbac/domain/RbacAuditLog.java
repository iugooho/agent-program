package com.travelagent.travelplanner.rbac.domain;

import java.time.LocalDateTime;

/**
 * RBAC 授权变更审计记录。
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
public record RbacAuditLog(
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
