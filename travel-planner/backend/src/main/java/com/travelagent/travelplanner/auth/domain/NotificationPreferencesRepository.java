package com.travelagent.travelplanner.auth.domain;

import java.util.Optional;

/** 通知偏好仓储。 */
public interface NotificationPreferencesRepository {

    /**
     * 查询通知偏好。
     *
     * @param userId 用户主键
     * @return 通知偏好
     */
    Optional<NotificationPreferences> findByUserId(long userId);

    /**
     * 保存通知偏好。
     *
     * @param preferences 通知偏好
     * @return 保存后的通知偏好
     */
    NotificationPreferences save(NotificationPreferences preferences);
}
