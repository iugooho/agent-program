package com.travelagent.travelplanner.rbac.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 更新角色数据范围请求。
 *
 * @param dataScope 数据范围
 * @param departmentIds 自定义部门主键
 */
public record RoleDataScopeRequest(
        @NotBlank String dataScope,
        @NotNull List<Long> departmentIds) {
}
