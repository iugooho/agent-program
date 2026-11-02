package com.travelagent.travelplanner.itinerary.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.travelagent.travelplanner.itinerary.application.TravelAgentService;
import com.travelagent.travelplanner.itinerary.application.TravelAgentService.PlanningResult;
import com.travelagent.travelplanner.itinerary.domain.BudgetSummary;
import com.travelagent.travelplanner.itinerary.domain.DayPlan;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.ItineraryItem;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 旅行规划接口的 MockMvc 测试。 */
@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TravelAgentService travelAgentService;

    @Test
    @DisplayName("POST /api/v1/trips/plan 返回结构化行程")
    void plansTrip() throws Exception {
        when(travelAgentService.plan("去杭州玩一天")).thenReturn(result());

        mockMvc.perform(post("/api/v1/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"去杭州玩一天\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.request.destinations[0]").value("杭州"))
                .andExpect(jsonPath("$.data.itinerary.days[0].items[0].title").value("西湖"))
                .andExpect(jsonPath("$.data.mode").value("rule+java"));
    }

    @Test
    @DisplayName("空需求返回统一参数错误响应")
    void rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1000));
    }

    private PlanningResult result() {
        TripRequest request = new TripRequest("北京", List.of("杭州"), LocalDate.now(), 1, 1,
                BigDecimal.valueOf(3000L), List.of("休闲"), "高铁", List.of(), List.of(), List.of());
        ItineraryItem item = new ItineraryItem("西湖", "景点", LocalTime.of(9, 0), LocalTime.of(11, 0),
                "杭州", BigDecimal.valueOf(60L), "提前预约");
        Itinerary itinerary = new Itinerary("杭州一日旅行计划", BigDecimal.valueOf(1000L),
                new BudgetSummary(BigDecimal.valueOf(60L), BigDecimal.valueOf(180L), BigDecimal.valueOf(760L),
                        BigDecimal.valueOf(1000L)),
                List.of(new DayPlan(1, LocalDate.now(), "杭州", List.of(item), List.of("晴"))),
                List.of("价格以实时查询为准"), List.of());
        return new PlanningResult(request, itinerary, "rule+java");
    }
}
