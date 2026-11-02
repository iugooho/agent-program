package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.itinerary.domain.DayPlan;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.ItineraryItem;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 在返回前端前执行确定性的业务校验，防止不可行方案进入后续流程。 */
@Component
public class ItineraryValidator {

    /** 校验需求字段。 */
    public void validateRequest(TripRequest request) {
        if (request == null || request.destinations() == null || request.destinations().isEmpty()
                || request.startDate() == null || request.budget() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "旅行需求缺少目的地、日期或预算");
        }
        if (request.days() < 1 || request.days() > 14) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "第一版仅支持 1 到 14 天的行程");
        }
        if (request.travelers() < 1 || request.travelers() > 20) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "同行人数必须在 1 到 20 人之间");
        }
        if (request.budget().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "预算必须大于 0");
        }
    }

    /** 校验生成结果的天数、时间顺序和预算。 */
    public void validateItinerary(TripRequest request, Itinerary itinerary) {
        if (itinerary == null || itinerary.days() == null || itinerary.days().size() != request.days()) {
            throw new BusinessException(ErrorCode.ITINERARY_GENERATION_FAILED, "生成的行程天数与需求不一致");
        }
        for (DayPlan day : itinerary.days()) {
            for (ItineraryItem item : day.items()) {
                if (item.startTime() == null || item.endTime() == null || !item.startTime().isBefore(item.endTime())) {
                    throw new BusinessException(ErrorCode.ITINERARY_GENERATION_FAILED, "行程包含无效的时间段");
                }
            }
            for (int index = 1; index < day.items().size(); index++) {
                if (day.items().get(index).startTime().isBefore(day.items().get(index - 1).endTime())) {
                    throw new BusinessException(ErrorCode.ITINERARY_GENERATION_FAILED, "同一天行程存在时间冲突");
                }
            }
        }
        if (itinerary.totalBudget() == null || itinerary.totalBudget().compareTo(request.budget()) > 0) {
            throw new BusinessException(ErrorCode.ITINERARY_GENERATION_FAILED,
                    "估算费用超过预算，请提高预算、减少天数或同行人数");
        }
    }
}
