package {{packageName}}.api;

/**
 * 统一响应结构：所有 REST 接口都返回 code + message + data。
 *
 * @param <T> 业务数据类型
 * @param code 业务码，0 表示成功
 * @param message 提示文案
 * @param data 业务数据
 */
public record ApiResponse<T>(int code, String message, T data) {

    /** 成功业务码。 */
    public static final int CODE_SUCCESS = 0;

    /**
     * 成功响应。
     *
     * @param data 业务数据
     * @param <T> 业务数据类型
     * @return 统一响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(CODE_SUCCESS, "ok", data);
    }

    /**
     * 失败响应。
     *
     * @param code 业务码
     * @param message 提示文案
     * @param <T> 业务数据类型
     * @return 统一响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 是否成功。
     *
     * @return code 为 0 时返回 true
     */
    public boolean success() {
        return code == CODE_SUCCESS;
    }
}
