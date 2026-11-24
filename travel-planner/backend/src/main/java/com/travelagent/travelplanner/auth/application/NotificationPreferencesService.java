package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.api.NotificationPreferencesRequest;
import com.travelagent.travelplanner.auth.domain.NotificationPreferences;
import com.travelagent.travelplanner.auth.domain.NotificationPreferencesRepository;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 通知偏好用例。 */
@Service
public class NotificationPreferencesService {

    private final UserRepository userRepository;
    private final NotificationPreferencesRepository preferencesRepository;

    /**
     * 创建通知偏好服务。
     *
     * @param userRepository 用户仓储
     * @param preferencesRepository 通知偏好仓储
     */
    public NotificationPreferencesService(
            UserRepository userRepository,
            NotificationPreferencesRepository preferencesRepository) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
    }

    /**
     * 查询通知偏好，不存在时返回默认值。
     *
     * @param userId 用户主键
     * @return 通知偏好
     */
    @Transactional(readOnly = true)
    public NotificationPreferences get(long userId) {
        requireUser(userId);
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> NotificationPreferences.defaults(userId));
    }

    /**
     * 更新通知偏好。
     *
     * @param userId 用户主键
     * @param request 更新请求
     * @return 保存后的通知偏好
     */
    @Transactional
    public NotificationPreferences update(long userId, NotificationPreferencesRequest request) {
        requireUser(userId);
        return preferencesRepository.save(new NotificationPreferences(
                userId,
                request.tripReminder(),
                request.tripChange(),
                request.systemMessage(),
                request.emailEnabled(),
                null));
    }

    private void requireUser(long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
    }
}
