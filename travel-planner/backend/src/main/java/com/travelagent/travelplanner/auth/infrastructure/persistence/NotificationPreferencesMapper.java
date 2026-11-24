package com.travelagent.travelplanner.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** user_notification_preferences 表 Mapper。 */
@Mapper
public interface NotificationPreferencesMapper {

    /**
     * 查询通知偏好。
     *
     * @param userId 用户主键
     * @return 通知偏好行
     */
    @Select("""
            SELECT user_id AS userId, trip_reminder AS tripReminder,
                   trip_change AS tripChange, system_message AS systemMessage,
                   email_enabled AS emailEnabled, update_time AS updateTime
            FROM user_notification_preferences
            WHERE user_id = #{userId}
            LIMIT 1
            """)
    NotificationPreferencesRow findByUserId(@Param("userId") long userId);

    /**
     * 新增通知偏好。
     *
     * @param userId 用户主键
     * @param tripReminder 行程提醒
     * @param tripChange 行程变更
     * @param systemMessage 系统消息
     * @param emailEnabled 邮件通知
     * @return 插入行数
     */
    @Insert("""
            INSERT INTO user_notification_preferences (
                user_id, trip_reminder, trip_change, system_message, email_enabled
            ) VALUES (
                #{userId}, #{tripReminder}, #{tripChange}, #{systemMessage}, #{emailEnabled}
            )
            """)
    int insert(
            @Param("userId") long userId,
            @Param("tripReminder") boolean tripReminder,
            @Param("tripChange") boolean tripChange,
            @Param("systemMessage") boolean systemMessage,
            @Param("emailEnabled") boolean emailEnabled);

    /**
     * 更新通知偏好。
     *
     * @param userId 用户主键
     * @param tripReminder 行程提醒
     * @param tripChange 行程变更
     * @param systemMessage 系统消息
     * @param emailEnabled 邮件通知
     * @return 更新行数
     */
    @Update("""
            UPDATE user_notification_preferences
            SET trip_reminder = #{tripReminder},
                trip_change = #{tripChange},
                system_message = #{systemMessage},
                email_enabled = #{emailEnabled},
                update_time = CURRENT_TIMESTAMP(6)
            WHERE user_id = #{userId}
            """)
    int update(
            @Param("userId") long userId,
            @Param("tripReminder") boolean tripReminder,
            @Param("tripChange") boolean tripChange,
            @Param("systemMessage") boolean systemMessage,
            @Param("emailEnabled") boolean emailEnabled);
}
