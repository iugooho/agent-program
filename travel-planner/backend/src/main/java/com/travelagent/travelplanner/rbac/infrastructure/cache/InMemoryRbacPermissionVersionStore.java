package com.travelagent.travelplanner.rbac.infrastructure.cache;

import com.travelagent.travelplanner.rbac.domain.RbacPermissionVersionStore;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 单实例授权版本存储。 */
@Component
@ConditionalOnProperty(name = "app.auth.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRbacPermissionVersionStore implements RbacPermissionVersionStore {

    private final AtomicLong version = new AtomicLong(1);

    @Override
    public long currentVersion() {
        return version.get();
    }

    @Override
    public long bumpVersion() {
        return version.incrementAndGet();
    }
}
