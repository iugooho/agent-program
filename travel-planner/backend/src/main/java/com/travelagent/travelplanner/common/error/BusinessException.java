package com.travelagent.travelplanner.common.error;

/**
 * 业务异常：抛出时带上 {@link ErrorCode}，由接口层的全局异常处理器统一转成 {@code ApiResponse}。
 *
 * <p>用来表达"可预期的失败"（查不到、状态不对、下游返回不可用），未预期的异常不要包成它，
 * 让全局处理器按 9000 打 error 日志更容易排查。</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * 用错误码自带的默认文案构造。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 覆盖默认文案，用于补充上下文（例如具体是哪个 ID）。
     *
     * @param errorCode 错误码
     * @param message 自定义文案，会原样返回给前端，不要塞堆栈或 SQL
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 错误码。
     *
     * @return 错误码
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
