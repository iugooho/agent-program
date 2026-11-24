package com.travelagent.travelplanner.auth.infrastructure.persistence;

import com.travelagent.travelplanner.auth.domain.SecurityEvent;
import com.travelagent.travelplanner.auth.domain.SecurityEventRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的安全事件仓储。 */
@Repository
public class MyBatisSecurityEventRepository implements SecurityEventRepository {

    private final SecurityEventMapper mapper;

    /**
     * 创建安全事件仓储。
     *
     * @param mapper 安全事件 Mapper
     */
    public MyBatisSecurityEventRepository(SecurityEventMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void create(
            long userId,
            String eventType,
            String description,
            String clientIp,
            String userAgent) {
        mapper.insert(userId, eventType, description, clientIp, userAgent);
    }

    @Override
    public List<SecurityEvent> findRecent(long userId, int limit) {
        return mapper.findRecent(userId, limit).stream()
                .map(row -> new SecurityEvent(
                        row.id(),
                        row.userId(),
                        row.eventType(),
                        row.description(),
                        row.clientIp(),
                        row.userAgent(),
                        row.createTime()))
                .toList();
    }
}
