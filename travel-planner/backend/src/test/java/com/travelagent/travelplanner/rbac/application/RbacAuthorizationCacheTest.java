package com.travelagent.travelplanner.rbac.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelagent.travelplanner.rbac.domain.EffectiveAuthorization;
import com.travelagent.travelplanner.rbac.domain.RbacPermissionVersionStore;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 授权版本缓存测试。 */
class RbacAuthorizationCacheTest {

    @Test
    @DisplayName("授权版本变化后重新加载用户权限")
    void reloadsAuthorizationWhenVersionChanges() {
        RbacRepository repository = mock(RbacRepository.class);
        RbacPermissionVersionStore versionStore = mock(RbacPermissionVersionStore.class);
        AtomicLong version = new AtomicLong(1);
        when(versionStore.currentVersion()).thenAnswer(invocation -> version.get());
        when(repository.findEffectiveAuthorization(7))
                .thenReturn(new EffectiveAuthorization(List.of("USER"), List.of("trip.read")))
                .thenReturn(new EffectiveAuthorization(List.of("ADMIN"), List.of("admin.user.read")));
        RbacAuthorizationCache cache = new RbacAuthorizationCache(repository, versionStore);

        EffectiveAuthorization first = cache.load(7);
        EffectiveAuthorization cached = cache.load(7);
        assertEquals(List.of("USER"), first.roles());
        assertEquals(first, cached);

        version.set(2);
        EffectiveAuthorization refreshed = cache.load(7);
        assertEquals(List.of("ADMIN"), refreshed.roles());
        verify(repository, times(2)).findEffectiveAuthorization(7);
    }
}
