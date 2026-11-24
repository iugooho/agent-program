package com.travelagent.travelplanner.rbac.domain;

import java.util.Set;

/**
 * 用户当前生效的数据范围。
 *
 * @param allData 是否可访问全部数据
 * @param selfAllowed 是否可访问本人数据
 * @param departmentIds 可访问部门主键
 */
public record RbacDataScope(boolean allData, boolean selfAllowed, Set<Long> departmentIds) {
}
