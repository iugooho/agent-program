package com.travelagent.travelplanner.auth.application;

import com.travelagent.travelplanner.auth.api.NotificationPreferencesResponse;
import com.travelagent.travelplanner.auth.api.UserDataExportResponse;
import com.travelagent.travelplanner.auth.api.UserPreferencesResponse;
import com.travelagent.travelplanner.auth.api.UserResponse;
import com.travelagent.travelplanner.auth.domain.UserAccount;
import com.travelagent.travelplanner.auth.domain.UserRepository;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.itinerary.api.SavedTravelPlanResponse;
import com.travelagent.travelplanner.itinerary.application.TravelPlanService;
import com.travelagent.travelplanner.rbac.domain.RbacRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 当前用户数据导出用例。 */
@Service
public class UserDataExportService {

    private final UserRepository userRepository;
    private final RbacRepository rbacRepository;
    private final UserProfileService userProfileService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final TravelPlanService travelPlanService;

    /**
     * 创建数据导出服务。
     *
     * @param userRepository 用户仓储
     * @param rbacRepository RBAC 仓储
     * @param userProfileService 用户资料服务
     * @param notificationPreferencesService 通知偏好服务
     * @param travelPlanService 旅行计划服务
     */
    public UserDataExportService(
            UserRepository userRepository,
            RbacRepository rbacRepository,
            UserProfileService userProfileService,
            NotificationPreferencesService notificationPreferencesService,
            TravelPlanService travelPlanService) {
        this.userRepository = userRepository;
        this.rbacRepository = rbacRepository;
        this.userProfileService = userProfileService;
        this.notificationPreferencesService = notificationPreferencesService;
        this.travelPlanService = travelPlanService;
    }

    /**
     * 导出当前用户可带走的数据。
     *
     * @param userId 用户主键
     * @return 数据包
     */
    @Transactional(readOnly = true)
    public UserDataExportResponse export(long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHENTICATED));
        List<SavedTravelPlanResponse> trips = travelPlanService.findOwned(userId).stream()
                .map(SavedTravelPlanResponse::from)
                .toList();
        return new UserDataExportResponse(
                UserResponse.from(user, rbacRepository.findEffectiveAuthorization(userId)),
                UserPreferencesResponse.from(userProfileService.getPreferences(userId)),
                NotificationPreferencesResponse.from(
                        notificationPreferencesService.get(userId)),
                trips);
    }
}
