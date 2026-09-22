package com.travelagent.travelplanner.api;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：把所有异常收敛成 {@link ApiResponse}，前端只需要认 {@code code} 一个字段。
 *
 * <p>放在接口层，业务模块不要自己 try-catch 拼响应。日志策略：可预期的失败打 warn（不带堆栈），
 * 未预期的异常打 error（带堆栈），前端一律只拿到 {@link ErrorCode#INTERNAL_ERROR} 的文案。
 * 错误码表见 {@code docs/error-codes.md}。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：业务代码主动抛出的可预期失败。
     *
     * @param exception 业务异常
     * @return 统一响应，HTTP 状态码由错误码决定
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        log.warn("业务异常 code={} message={}", errorCode.code(), exception.getMessage());
        return build(errorCode, exception.getMessage());
    }

    /**
     * 请求体参数校验失败（{@code @Valid} 不通过）。
     *
     * @param exception 校验异常
     * @return 统一响应，message 为「字段名 + 原因」
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse(ErrorCode.PARAM_INVALID.message());
        log.warn("参数校验失败 message={}", message);
        return build(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 查询参数 / 路径参数校验失败（{@code @Validated} 不通过）。
     *
     * @param exception 校验异常
     * @return 统一响应，message 为框架给出的约束说明
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        log.warn("参数校验失败 message={}", exception.getMessage());
        return build(ErrorCode.PARAM_INVALID, exception.getMessage());
    }

    /**
     * 请求体读不出来：不是合法 JSON、字段类型不匹配等。
     *
     * @param exception 解析异常
     * @return 统一响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败 message={}", exception.getMessage());
        return build(ErrorCode.BODY_UNREADABLE, ErrorCode.BODY_UNREADABLE.message());
    }

    /**
     * 路径存在但 HTTP 方法不对。
     *
     * @param exception 方法不支持异常
     * @return 统一响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("请求方法不支持 method={} message={}", exception.getMethod(), exception.getMessage());
        return build(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.message());
    }

    /**
     * 路径没有匹配到任何接口。
     *
     * @param exception 资源未找到异常
     * @return 统一响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        log.warn("接口不存在 path={}", exception.getResourcePath());
        return build(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.message());
    }

    /**
     * 路径没有匹配到任何接口（静态资源处理器也没接住时走这里）。
     *
     * @param exception 未找到处理器异常
     * @return 统一响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException exception) {
        log.warn("接口不存在 url={}", exception.getRequestURL());
        return build(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.message());
    }

    /**
     * 兜底：未预期的异常。堆栈只进日志，不回给前端。
     *
     * @param exception 未预期的异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
        log.error("未预期的异常", exception);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }

    /**
     * 按错误码组装统一响应。
     *
     * @param errorCode 错误码
     * @param message 返回给前端的文案
     * @return 带对应 HTTP 状态的统一响应
     */
    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus())
                .body(ApiResponse.error(errorCode.code(), message));
    }
}
