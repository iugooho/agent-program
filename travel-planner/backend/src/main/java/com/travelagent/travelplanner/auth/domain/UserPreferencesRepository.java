package com.travelagent.travelplanner.auth.domain;

import java.util.Optional;

/** 用户长期偏好仓储。 */
public interface UserPreferencesRepository {

    /**
     * 查询用户偏好。
     *
     * @param userId 用户主键
     * @return 用户偏好
     */
    Optional<UserPreferences> findByUserId(long userId);

    /**
     * 保存用户偏好，不存在时创建。
     *
     * @param preferences 用户偏好
     * @return 保存后的偏好
     */
    UserPreferences save(UserPreferences preferences);
}
