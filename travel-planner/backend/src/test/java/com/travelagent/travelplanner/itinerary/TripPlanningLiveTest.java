package com.travelagent.travelplanner.itinerary;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** 可选的真实服务冒烟测试，只在显式提供密钥和开关时运行。 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_TESTS", matches = "true")
class TripPlanningLiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("真实调用 DeepSeek、高德和和风完成旅行规划")
    void plansTripWithLiveProviders() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/trips/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"从北京去杭州玩 4 天，2 人，预算 8000，想轻松一点，不吃辣，不要购物\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.mode").value("deepseek+java"))
                .andExpect(jsonPath("$.data.request.destinations[0]").value("杭州"))
                .andExpect(jsonPath("$.data.itinerary.days.length()").value(4))
                .andExpect(jsonPath("$.data.itinerary.evidences.length()").value(2));
    }
}
