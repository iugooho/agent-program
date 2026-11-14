package com.travelagent.travelplanner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 统一分页响应测试。 */
class PageResultTest {

    @Test
    @DisplayName("分页响应复制数据项并保留分页信息")
    void createsImmutablePageResult() {
        PageResult<String> result = new PageResult<>(1, 20, 45, List.of("a", "b"));

        assertEquals(1, result.page());
        assertEquals(20, result.size());
        assertEquals(45, result.total());
        assertEquals(List.of("a", "b"), result.items());
    }

    @Test
    @DisplayName("非法页码和每页条数直接拒绝")
    void rejectsInvalidPagination() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(0, 20, 0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(1, 0, 0, List.of()));
    }
}
