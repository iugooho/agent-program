package com.travelagent.travelplanner.rbac.application;

import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionVersionStore;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 按授权版本缓存用户有效角色和权限。 */
@Component
public class RbacAuthorizationCache {

    private final RbacRepository repository;
    private final RbacPermissionVersionStore versionStore;
    private final ConcurrentHashMap<Long, EffectiveAuthorization> authorizations =
            new ConcurrentHashMap<>();
    private volatile long loadedVersion = Long.MIN_VALUE;

    /**
     * 创建授权缓存。
     *
     * @param repository RBAC 仓储
     * @param versionStore 授权版本存储
     */
    public RbacAuthorizationCache(
            RbacRepository repository,
            RbacPermissionVersionStore versionStore) {
        this.repository = repository;
        this.versionStore = versionStore;
    }

    /**
     * 读取用户当前有效授权。
     *
     * @param userId 用户主键
     * @return 有效授权
     */
    public EffectiveAuthorization load(long userId) {
        while (true) {
            long version = versionStore.currentVersion();
            refreshIfNeeded(version);
            EffectiveAuthorization authorization = authorizations.computeIfAbsent(
                    userId,
                    repository::findEffectiveAuthorization);
            if (version == versionStore.currentVersion()) {
                return authorization;
            }
        }
    }

    private void refreshIfNeeded(long version) {
        if (version == loadedVersion) {
            return;
        }
        synchronized (this) {
            if (version != loadedVersion) {
                authorizations.clear();
                loadedVersion = version;
            }
        }
    }
}
