package com.travelagent.travelplanner.rbac.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新用户所属部门请求。
 *
 * @param departmentIds 部门主键
 */
public record UserDepartmentsRequest(@NotNull List<Long> departmentIds) {
}
