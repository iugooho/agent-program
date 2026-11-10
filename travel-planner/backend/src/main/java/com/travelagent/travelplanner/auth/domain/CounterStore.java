package com.travelagent.travelplanner.auth.domain;

import java.time.Duration;

/** 限流和风险控制使用的原子计数器。 */
public interface CounterStore {

    /**
     * 递增计数器，首次创建时设置有效期。
     *
     * @param key 键
     * @param ttl 有效期
     * @return 递增后的值
     */
    long increment(String key, Duration ttl);

    /**
     * 读取当前计数。
     *
     * @param key 键
     * @return 当前值，不存在时返回 0
     */
    long get(String key);

    /**
     * 清除计数器。
     *
     * @param key 键
     */
    void delete(String key);
}
