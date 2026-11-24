package com.travelagent.travelplanner.rbac.domain;

/**
 * 部门。
 *
 * @param id 主键
 * @param parentId 父部门主键
 * @param code 部门编码
 * @param name 部门名称
 * @param leaderUserId 负责人用户主键
 * @param status 状态
 * @param sortOrder 排序值
 */
public record RbacDepartment(
        long id,
        Long parentId,
        String code,
        String name,
        Long leaderUserId,
        String status,
        int sortOrder) {
}
