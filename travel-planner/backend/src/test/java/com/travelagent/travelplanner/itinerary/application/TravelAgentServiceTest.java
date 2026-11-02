package com.travelagent.travelplanner.itinerary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.AttractionFact;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.TravelContext;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.WeatherFact;
import com.travelagent.travelplanner.itinerary.domain.ToolEvidence;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 验证智能体在模型不可用时仍能完成完整规划闭环。 */
class TravelAgentServiceTest {

    @Test
    @DisplayName("模型不可用时使用规则解析并生成经过校验的行程")
    void plansWithRuleFallback() {
        RequirementModel unavailableModel = message -> Optional.empty();
        TravelDataGateway gateway = request -> new TravelContext(
                List.of(new AttractionFact("杭州", "西湖", "风景名胜", "西湖区", BigDecimal.valueOf(60L)),
                        new AttractionFact("杭州", "灵隐寺", "人文景观", "灵隐路", BigDecimal.valueOf(75L))),
                List.of(new WeatherFact(LocalDate.now().plusDays(7), "多云", "18~25°C", true)),
                List.of(new ToolEvidence("测试数据", "单元测试固定数据", Instant.now())),
                List.of());
        TravelAgentService service = new TravelAgentService(unavailableModel, new RuleBasedRequirementParser(),
                gateway, new ItineraryComposer(), new ItineraryValidator());

        TravelAgentService.PlanningResult result = service.plan("从北京去杭州玩 2 天，预算 5000，想轻松一点");

        assertThat(result.mode()).isEqualTo("rule+java");
        assertThat(result.request().destinations()).containsExactly("杭州");
        assertThat(result.itinerary().days()).hasSize(2);
        assertThat(result.itinerary().totalBudget()).isLessThanOrEqualTo(result.request().budget());
        assertThat(result.itinerary().days().get(0).items()).hasSize(4);
    }
}
