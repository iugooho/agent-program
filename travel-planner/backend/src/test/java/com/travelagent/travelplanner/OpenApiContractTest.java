package com.travelagent.travelplanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OpenAPI 契约测试：把后端实际输出的接口契约固化成 {@code docs/openapi.json}，作为前后端唯一可信源。
 *
 * <p>改了 Controller 或 DTO，这里立刻会红，逼着把契约一起更新；前端再从契约生成 TypeScript 类型，
 * 于是"后端改字段名、前端到运行时才发现"这类问题提前到编译期。</p>
 *
 * <p>更新契约：{@code mvn test -Dopenapi.write=true}，然后把 {@code docs/openapi.json} 一起提交。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    /** 契约文件位置，相对 backend 模块目录。 */
    private static final Path CONTRACT = Path.of("..", "docs", "openapi.json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 校验后端输出的 OpenAPI 文档与已提交的契约文件一致。
     *
     * @throws Exception 读写契约文件或调用接口失败
     */
    @Test
    @DisplayName("后端实际输出的 OpenAPI 与 docs/openapi.json 一致")
    void openApiSpecMatchesCommittedContract() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode actual = objectMapper.readTree(body);
        assertThat(actual).as("springdoc 没有输出可解析的 OpenAPI 文档").isNotNull();

        if (Boolean.getBoolean("openapi.write")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n",
                    StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRACT)
                .as("契约文件缺失，执行 mvn test -Dopenapi.write=true 生成 %s", CONTRACT)
                .exists();
        JsonNode expected = objectMapper.readTree(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        assertThat(actual)
                .as("后端 OpenAPI 与 %s 不一致：接口改了就要重新生成契约并同步给前端", CONTRACT)
                .isEqualTo(expected);
    }

    /**
     * 契约里只能出现正式接口，测试用的 Controller 不能漏进来。
     *
     * @throws Exception 调用接口失败
     */
    @Test
    @DisplayName("OpenAPI 里的路径都在 /api/v1 下，没有测试用的接口混进来")
    void openApiOnlyExposesVersionedApiPaths() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        List<String> paths = new ArrayList<>();
        objectMapper.readTree(body).path("paths").fieldNames().forEachRemaining(paths::add);

        assertThat(paths)
                .as("契约里的接口路径都应以 /api/v1 开头；测试专用 Controller 记得加 @Profile 或 @TestConfiguration")
                .isNotEmpty()
                .allMatch(path -> path.startsWith("/api/v1"));
    }
}
