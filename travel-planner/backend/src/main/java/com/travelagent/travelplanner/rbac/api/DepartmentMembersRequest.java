package com.travelagent.travelplanner.rbac.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新部门成员请求。
 *
 * @param userIds 用户主键
 */
public record DepartmentMembersRequest(@NotNull List<Long> userIds) {
}
