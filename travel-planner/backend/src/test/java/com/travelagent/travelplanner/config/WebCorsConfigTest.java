package com.travelagent.travelplanner.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** 前端开发来源的跨域回归测试。 */
@SpringBootTest
@AutoConfigureMockMvc
class WebCorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("允许 travel-planner 的 5174 开发来源")
    void allowsTravelPlannerDevOrigin() throws Exception {
        String origin = "http://127.0.0.1:5174";

        mockMvc.perform(get("/api/v1/health").header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));

    }
}
