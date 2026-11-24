package com.travelagent.travelplanner.auth.domain;

import java.util.List;

/** 账号安全事件仓储。 */
public interface SecurityEventRepository {

    /**
     * 写入安全事件。
     *
     * @param userId 用户主键
     * @param eventType 事件类型
     * @param description 描述
     * @param clientIp 客户端 IP
     * @param userAgent 客户端标识
     */
    void create(
            long userId,
            String eventType,
            String description,
            String clientIp,
            String userAgent);

    /**
     * 查询用户最近安全事件。
     *
     * @param userId 用户主键
     * @param limit 最大条数
     * @return 安全事件
     */
    List<SecurityEvent> findRecent(long userId, int limit);
}
