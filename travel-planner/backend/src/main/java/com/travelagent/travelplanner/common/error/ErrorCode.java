package com.travelagent.travelplanner.common.error;

/**
 * 业务错误码表：前端只认 {@code code}，不解析 message 文案。
 *
 * <p>段位划分（新增模块先挑一个没被占用的段位，再在 {@code docs/error-codes.md} 登记）：</p>
 * <ul>
 *   <li>10xx 通用：参数、请求格式、路由、鉴权</li>
 *   <li>20xx 行程 itinerary</li>
 *   <li>21xx 用户与登录（预留）</li>
 *   <li>90xx 系统与依赖</li>
 * </ul>
 *
 * <p>成功码 0 定义在 {@code ApiResponse.CODE_SUCCESS}，不在这里重复。本枚举刻意不引入任何框架类型
 * （HTTP 状态用 int 表达），这样 domain 与 application 层抛 {@link BusinessException} 时不会带来 Web 依赖。</p>
 */
public enum ErrorCode {

    /** 参数校验失败：@Valid 不通过，message 里会带上字段名。 */
    PARAM_INVALID(1000, "参数校验失败", 400),

    /** 请求体格式错误：body 不是合法 JSON 或字段类型不匹配。 */
    BODY_UNREADABLE(1001, "请求体格式错误", 400),

    /** 资源不存在：路径没匹配到任何接口，或按 ID 查不到数据。 */
    RESOURCE_NOT_FOUND(1002, "资源不存在", 404),

    /** 请求方法不支持：路径存在但 HTTP 方法不对。 */
    METHOD_NOT_ALLOWED(1003, "请求方法不支持", 405),

    /** 未认证或登录状态已失效：预留，等登录落地后由 Security 层返回。 */
    UNAUTHENTICATED(1004, "未认证或登录状态已失效", 401),

    /** 没有访问权限：预留，同上。 */
    FORBIDDEN(1005, "没有访问权限", 403),

    /** 行程不存在。 */
    ITINERARY_NOT_FOUND(2000, "行程不存在", 404),

    /** 行程生成失败：模型或外部工具的结果无法组装成行程。 */
    ITINERARY_GENERATION_FAILED(2001, "行程生成失败", 422),

    /** 服务器内部错误：未预期的异常，细节只进日志。 */
    INTERNAL_ERROR(9000, "服务器内部错误", 500);

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
