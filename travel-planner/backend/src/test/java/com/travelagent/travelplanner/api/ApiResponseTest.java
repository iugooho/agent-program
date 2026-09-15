package com.travelagent.travelplanner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 统一响应结构的冒烟测试，保证 mvn test 开箱可用。 */
class ApiResponseTest {

    @Test
    @DisplayName("ok 返回成功码与业务数据")
    void okCarriesData() {
        ApiResponse<String> response = ApiResponse.ok("travel-planner");

        assertTrue(response.success());
        assertEquals(ApiResponse.CODE_SUCCESS, response.code());
        assertEquals("travel-planner", response.data());
    }

    @Test
    @DisplayName("error 返回业务码与提示文案")
    void errorCarriesMessage() {
        ApiResponse<Void> response = ApiResponse.error(40001, "参数不合法");

        assertFalse(response.success());
        assertEquals(40001, response.code());
        assertEquals("参数不合法", response.message());
    }
}
