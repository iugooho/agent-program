package {{packageName}}.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 健康检查接口的 MockMvc 集成测试：走真实的 Spring MVC 分发链路，
 * 但不绑定端口、不启动 Tomcat，所以在 CI 里可以稳定跑通。
 *
 * <p>addFilters = false 是为了让测试专注在接口本身：选了 Spring Security 时，
 * 过滤链会拦下未携带凭证的请求，这里跳过过滤器避免依赖登录态。</p>
 */
@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health 返回 code=0、服务名与 UP 状态")
    void healthReturnsUnifiedPayload() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.service").value("{{backendArtifactId}}"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}
