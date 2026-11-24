package com.travelagent.travelplanner.itinerary.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.itinerary.application.TravelAgentService.PlanningResult;
import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import com.travelagent.travelplanner.rbac.application.RbacDataScopeService;
import com.travelagent.travelplanner.rbac.domain.RbacDataScope;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 持久化旅行计划并执行资源数据权限。 */
@Service
public class TravelPlanService {

    private final TravelPlanRepository repository;
    private final RbacDataScopeService dataScopeService;
    private final ObjectMapper objectMapper;

    /**
     * 创建旅行计划服务。
     *
     * @param repository 计划仓储
     * @param dataScopeService 数据范围服务
     * @param objectMapper JSON 工具
     */
    public TravelPlanService(
            TravelPlanRepository repository,
            RbacDataScopeService dataScopeService,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存规划结果。
     *
     * @param userId 当前用户主键
     * @param result 规划结果
     * @return 保存后的计划
     */
    @Transactional
    public SavedTravelPlan save(long userId, PlanningResult result) {
        try {
            return repository.save(
                    userId,
                    dataScopeService.primaryDepartmentId(userId),
                    objectMapper.writeValueAsString(result.request()),
                    objectMapper.writeValueAsString(result.itinerary()),
                    result.mode());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行计划 JSON 序列化失败", exception);
        }
    }

    /**
     * 查询当前用户可见计划。
     *
     * @param userId 用户主键
     * @return 可见计划
     */
    @Transactional(readOnly = true)
    public List<SavedTravelPlan> findVisible(long userId) {
        RbacDataScope scope = dataScopeService.resolve(userId);
        return repository.findVisible(
                userId,
                scope.allData(),
                scope.selfAllowed(),
                scope.departmentIds());
    }

    /**
     * 查询可访问的单个计划。
     *
     * @param userId 当前用户主键
     * @param planId 计划主键
     * @return 计划
     */
    @Transactional(readOnly = true)
    public SavedTravelPlan requireVisible(long userId, long planId) {
        SavedTravelPlan plan = repository.findById(planId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "旅行计划不存在"));
        if (!dataScopeService.canAccess(userId, plan.ownerId(), plan.departmentId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "旅行计划不存在");
        }
        return plan;
    }

    /**
     * 删除可访问的计划。
     *
     * @param userId 当前用户主键
     * @param planId 计划主键
     */
    @Transactional
    public void delete(long userId, long planId) {
        requireVisible(userId, planId);
        repository.delete(planId);
    }
}
