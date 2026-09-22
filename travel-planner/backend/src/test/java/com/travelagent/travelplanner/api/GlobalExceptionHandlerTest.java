package com.travelagent.travelplanner.api;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelagent.travelplanner.common.error.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 全局异常处理的测试：借 {@link ErrorProbeController} 触发各类异常，确认它们都被转成统一响应，
 * 且业务码、HTTP 状态与 {@code docs/error-codes.md} 里的表一致。
 *
 * <p>{@code @WebMvcTest} 本身会把 {@code @RestControllerAdvice} 装进上下文，所以不需要手动 @Import 处理器。</p>
 */
@WebMvcTest(ErrorProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("error-probe")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("业务异常按错误码转成统一响应，HTTP 状态取码表里的值")
    void businessExceptionUsesErrorCode() throws Exception {
        mockMvc.perform(get("/probe/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ITINERARY_NOT_FOUND.code()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ITINERARY_NOT_FOUND.message()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("业务异常可以覆盖文案，但业务码不变")
    void businessExceptionKeepsCustomMessage() throws Exception {
        mockMvc.perform(get("/probe/business-custom"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ITINERARY_NOT_FOUND.code()))
                .andExpect(jsonPath("$.message").value("行程 t-1 不存在"));
    }

    @Test
    @DisplayName("请求体校验失败返回 1000，message 带字段名")
    void validationFailureReturnsParamInvalid() throws Exception {
        mockMvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.code()))
                .andExpect(jsonPath("$.message").value(startsWith("name")));
    }

    @Test
    @DisplayName("请求体不是合法 JSON 返回 1001")
    void unreadableBodyReturnsBodyUnreadable() throws Exception {
        mockMvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.BODY_UNREADABLE.code()))
                .andExpect(jsonPath("$.message").value(ErrorCode.BODY_UNREADABLE.message()));
    }

    @Test
    @DisplayName("请求方法不支持返回 1003")
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/probe/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.code()));
    }

    @Test
    @DisplayName("未预期的异常返回 9000，且不把堆栈细节回给前端")
    void unexpectedExceptionReturnsInternalError() throws Exception {
        mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.code()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.message()));
    }

    @Test
    @DisplayName("路径不存在也走统一响应，返回 1002")
    void unknownPathReturnsResourceNotFound() throws Exception {
        mockMvc.perform(get("/probe/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.code()));
    }
}
