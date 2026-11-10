package com.travelagent.travelplanner.common.error;

/**
 * 业务错误码表：前端只认 {@code code}，不解析 message 文案。
 *
 * <p>认证相关码值严格遵循接口设计文档；参数、路由和系统错误继续使用不冲突的扩展编号。
 * 前端只根据 {@code code} 判断分支，文案可以调整但码值不能复用。</p>
 */
public enum ErrorCode {

    /** 参数校验失败：字段缺失、格式错误或组合条件不满足。 */
    PARAM_INVALID(1000, "参数校验失败", 400),

    /** 请求体格式错误：body 不是合法 JSON 或字段类型不匹配。 */
    BODY_UNREADABLE(1008, "请求体格式错误", 400),

    /** 请求方法不支持。 */
    METHOD_NOT_ALLOWED(1009, "请求方法不支持", 405),

    /** 未登录、访问令牌无效或已经失效。 */
    UNAUTHENTICATED(401, "未登录或令牌失效", 401),

    /** 已登录但没有访问权限。 */
    FORBIDDEN(403, "无访问权限", 403),

    /** 资源不存在。 */
    RESOURCE_NOT_FOUND(404, "资源不存在", 404),

    /** 注册账号已存在。 */
    ACCOUNT_EXISTS(1001, "账号已存在", 409),

    /** 注册邮箱已存在。 */
    EMAIL_EXISTS(1002, "邮箱已存在", 409),

    /** 登录账号或密码错误。 */
    BAD_CREDENTIALS(1003, "账号或密码错误", 401),

    /** 邮箱验证码错误、已过期或已使用。 */
    EMAIL_CODE_INVALID(1004, "邮箱验证码错误或已过期", 400),

    /** 账号已被禁用。 */
    ACCOUNT_DISABLED(1005, "账号已禁用", 403),

    /** Refresh Token 无效或已经撤销。 */
    REFRESH_TOKEN_INVALID(1006, "Refresh Token 无效或已撤销", 401),

    /** 邮箱验证码发送过于频繁。 */
    EMAIL_CODE_TOO_FREQUENT(1007, "邮箱验证码发送过于频繁", 429),

    /** 当前操作需要先完成人机验证。 */
    CAPTCHA_REQUIRED(1010, "请先完成人机验证", 428),

    /** 人机验证答案或票据无效、已过期或已使用。 */
    CAPTCHA_INVALID(1011, "人机验证已失效，请重新验证", 400),

    /** 通用接口请求过于频繁。 */
    RATE_LIMITED(1012, "请求过于频繁，请稍后重试", 429),

    /** 行程不存在。 */
    ITINERARY_NOT_FOUND(2000, "行程不存在", 404),

    /** 行程生成失败：模型或外部工具的结果无法组装成行程。 */
    ITINERARY_GENERATION_FAILED(2001, "行程生成失败", 422),

    /** 服务器内部错误：未预期的异常，细节只进日志。 */
    INTERNAL_ERROR(500, "内部服务器错误", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /**
     * 业务码。
     *
     * @return 业务码
     */
    public int code() {
        return code;
    }

    /**
     * 默认提示文案。
     *
     * @return 默认提示文案
     */
    public String message() {
        return message;
    }

    /**
     * 该错误码对应的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int httpStatus() {
        return httpStatus;
    }
}
