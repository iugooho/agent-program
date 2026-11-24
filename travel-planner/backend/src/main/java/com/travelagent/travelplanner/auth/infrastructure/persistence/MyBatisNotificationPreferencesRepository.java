package com.travelagent.travelplanner.auth.infrastructure.persistence;

import com.travelagent.travelplanner.auth.domain.NotificationPreferences;
import com.travelagent.travelplanner.auth.domain.NotificationPreferencesRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的通知偏好仓储。 */
@Repository
public class MyBatisNotificationPreferencesRepository implements NotificationPreferencesRepository {

    private final NotificationPreferencesMapper mapper;

    /**
     * 创建通知偏好仓储。
     *
     * @param mapper 通知偏好 Mapper
     */
    public MyBatisNotificationPreferencesRepository(NotificationPreferencesMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<NotificationPreferences> findByUserId(long userId) {
        return Optional.ofNullable(mapper.findByUserId(userId)).map(this::toDomain);
    }

    @Override
    public NotificationPreferences save(NotificationPreferences preferences) {
        if (mapper.update(
                preferences.userId(),
                preferences.tripReminder(),
                preferences.tripChange(),
                preferences.systemMessage(),
                preferences.emailEnabled()) == 0) {
            mapper.insert(
                    preferences.userId(),
                    preferences.tripReminder(),
                    preferences.tripChange(),
                    preferences.systemMessage(),
                    preferences.emailEnabled());
        }
        return findByUserId(preferences.userId())
                .orElseThrow(() -> new IllegalStateException("通知偏好保存后未能查询到"));
    }

    private NotificationPreferences toDomain(NotificationPreferencesRow row) {
        return new NotificationPreferences(
                row.userId(),
                row.tripReminder(),
                row.tripChange(),
                row.systemMessage(),
                row.emailEnabled(),
                row.updateTime());
    }
}
