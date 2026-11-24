package com.travelagent.travelplanner.itinerary.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.itinerary.application.TravelPlanRepository;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.SavedTravelPlan;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的旅行计划仓储。 */
@Repository
public class MyBatisTravelPlanRepository implements TravelPlanRepository {

    private final TravelPlanMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建旅行计划仓储。
     *
     * @param mapper Mapper
     * @param objectMapper JSON 工具
     */
    public MyBatisTravelPlanRepository(
            TravelPlanMapper mapper,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public SavedTravelPlan save(
            long ownerId,
            Long departmentId,
            String requestJson,
            String itineraryJson,
            String mode) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ownerId", ownerId);
        parameters.put("departmentId", departmentId);
        parameters.put("requestPayload", requestJson);
        parameters.put("itineraryPayload", itineraryJson);
        parameters.put("mode", mode);
        mapper.insert(parameters);
        Object generatedId = parameters.get("id");
        if (!(generatedId instanceof Number id)) {
            throw new IllegalStateException("旅行计划保存后未返回主键");
        }
        return Optional.ofNullable(mapper.findById(id.longValue()))
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("旅行计划保存后未能查询到"));
    }

    @Override
    public List<SavedTravelPlan> findVisible(
            long userId,
            boolean allData,
            boolean selfAllowed,
            Set<Long> departmentIds) {
        return mapper.findVisible(userId, allData, selfAllowed, departmentIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SavedTravelPlan> findByOwner(long ownerId) {
        return mapper.findByOwner(ownerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<SavedTravelPlan> findById(long planId) {
        return Optional.ofNullable(mapper.findById(planId)).map(this::toDomain);
    }

    @Override
    public void delete(long planId) {
        mapper.delete(planId);
    }

    private SavedTravelPlan toDomain(TravelPlanRow row) {
        try {
            return new SavedTravelPlan(
                    row.id(),
                    row.ownerId(),
                    row.departmentId(),
                    objectMapper.readValue(row.requestPayload(), TripRequest.class),
                    objectMapper.readValue(row.itineraryPayload(), Itinerary.class),
                    row.mode(),
                    row.createTime());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("旅行计划 JSON 解析失败", exception);
        }
    }
}
