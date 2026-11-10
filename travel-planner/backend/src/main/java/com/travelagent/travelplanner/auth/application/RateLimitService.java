package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.domain.CounterStore;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import java.time.Duration;
import org.springframework.stereotype.Service;

/** 通用接口限流服务。 */
@Service
public class RateLimitService {

    private final CounterStore counterStore;

    public RateLimitService(CounterStore counterStore) {
        this.counterStore = counterStore;
    }

    /**
     * 判断请求是否仍在额度内。
     *
     * @param bucket 限流桶
     * @param identity 客户端标识
     * @param limit 窗口上限
     * @param window 窗口长度
     * @return 是否允许
     */
    public boolean isAllowed(String bucket, String identity, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        return counterStore.increment(bucket + ":" + identity, window) <= limit;
    }

    /**
     * 额度不足时抛出统一 429。
     *
     * @param bucket 限流桶
     * @param identity 客户端标识
     * @param limit 窗口上限
     * @param window 窗口长度
     */
    public void requireAllowed(String bucket, String identity, int limit, Duration window) {
        if (!isAllowed(bucket, identity, limit, window)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
    }
}
