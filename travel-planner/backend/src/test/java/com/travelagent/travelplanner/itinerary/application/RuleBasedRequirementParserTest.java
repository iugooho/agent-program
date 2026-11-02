package com.travelagent.travelplanner.itinerary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 验证无模型环境下仍能解析常见的旅行需求。 */
class RuleBasedRequirementParserTest {

    private final RuleBasedRequirementParser parser = new RuleBasedRequirementParser();

    @Test
    @DisplayName("解析目的地、人数、天数、预算和偏好")
    void parsesCommonChineseRequest() {
        TripRequest request = parser.parse("从北京去杭州玩 4 天，2 人，预算 6000，想轻松一点，不要购物");

        assertThat(request.departureCity()).isEqualTo("北京");
        assertThat(request.destinations()).containsExactly("杭州");
        assertThat(request.days()).isEqualTo(4);
        assertThat(request.travelers()).isEqualTo(2);
        assertThat(request.budget()).isEqualByComparingTo("6000");
        assertThat(request.travelStyles()).contains("休闲");
        assertThat(request.forbiddenItems()).contains("购物");
    }

    @Test
    @DisplayName("万为单位的预算可以正确换算")
    void parsesBudgetInTenThousands() {
        TripRequest request = parser.parse("去成都玩 3 天，预算 1.5 万");

        assertThat(request.destinations()).containsExactly("成都");
        assertThat(request.budget()).isEqualByComparingTo("15000");
    }
}
