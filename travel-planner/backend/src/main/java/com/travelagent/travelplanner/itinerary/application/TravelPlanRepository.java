package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 旅行计划持久化接口。 */
public interface TravelPlanRepository {

    /**
     * 保存旅行计划。
     *
     * @param ownerId 所有者主键
     * @param departmentId 所属部门主键
     * @param requestJson 需求 JSON
     * @param itineraryJson 行程 JSON
     * @param mode 生成模式
     * @return 保存后的计划
     */
    SavedTravelPlan save(
            long ownerId,
            Long departmentId,
            String requestJson,
            String itineraryJson,
            String mode);

    /**
     * 查询用户可见计划。
     *
     * @param userId 当前用户主键
     * @param allData 是否全部数据
     * @param selfAllowed 是否允许本人数据
     * @param departmentIds 可见部门主键
     * @return 旅行计划
     */
    List<SavedTravelPlan> findVisible(
            long userId,
            boolean allData,
            boolean selfAllowed,
            Set<Long> departmentIds);

    /**
     * 按主键查询计划。
     *
     * @param planId 计划主键
     * @return 旅行计划
     */
    Optional<SavedTravelPlan> findById(long planId);

    /**
     * 删除计划。
     *
     * @param planId 计划主键
     */
    void delete(long planId);
}
