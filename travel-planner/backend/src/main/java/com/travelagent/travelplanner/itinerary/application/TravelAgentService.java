package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.TravelContext;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 旅行规划智能体：模型负责理解，工具负责事实，Java 负责计算和校验。 */
@Service
public class TravelAgentService {

    private final RequirementModel requirementModel;
    private final RuleBasedRequirementParser ruleBasedParser;
    private final TravelDataGateway travelDataGateway;
    private final ItineraryComposer itineraryComposer;
    private final ItineraryValidator itineraryValidator;

    /** 创建旅行规划智能体。 */
    public TravelAgentService(RequirementModel requirementModel, RuleBasedRequirementParser ruleBasedParser,
                              TravelDataGateway travelDataGateway, ItineraryComposer itineraryComposer,
                              ItineraryValidator itineraryValidator) {
        this.requirementModel = requirementModel;
        this.ruleBasedParser = ruleBasedParser;
        this.travelDataGateway = travelDataGateway;
        this.itineraryComposer = itineraryComposer;
        this.itineraryValidator = itineraryValidator;
    }

    /** 执行一次完整的同步规划。 */
    public PlanningResult plan(String message) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请描述目的地、天数和旅行偏好");
        }
        Optional<TripRequest> modelRequest = requirementModel.extract(message);
        TripRequest request = modelRequest.orElseGet(() -> ruleBasedParser.parse(message));
        itineraryValidator.validateRequest(request);
        TravelContext context = travelDataGateway.collect(request);
        Itinerary itinerary = itineraryComposer.compose(request, context);
        itineraryValidator.validateItinerary(request, itinerary);
        return new PlanningResult(request, itinerary, modelRequest.isPresent() ? "deepseek+java" : "rule+java");
    }

    /** 智能体执行结果。 */
    public record PlanningResult(TripRequest request, Itinerary itinerary, String mode) {
    }
}
