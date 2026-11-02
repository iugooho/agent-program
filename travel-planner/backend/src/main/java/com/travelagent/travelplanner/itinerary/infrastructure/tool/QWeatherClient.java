package com.travelagent.travelplanner.itinerary.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.WeatherFact;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 和风天气适配器，使用项目专属 API Host 与 API KEY 请求头。 */
@Component
public class QWeatherClient {

    private static final Set<String> BAD_WEATHER = Set.of("雨", "雪", "雷", "雾", "霾", "沙尘", "暴");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiHost;
    private final String apiKey;

    /** 创建天气客户端。 */
    public QWeatherClient(
            ObjectMapper objectMapper,
            @Value("${app.travel.weather.base-url:}") String apiHost,
            @Value("${app.travel.weather.api-key:}") String apiKey) {
        this.objectMapper = objectMapper;
        this.apiHost = apiHost == null ? "" : apiHost.replaceAll("/+$", "");
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    /** 获取城市未来七天天气预报。 */
    public List<WeatherFact> forecast(String location) {
        if (apiHost.isBlank() || apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        byte[] body = restClient.get()
                .uri(apiHost + "/v7/weather/7d?location={location}&lang=zh", location)
                .headers(headers -> {
                    headers.set("X-QW-Api-Key", apiKey);
                    headers.set("Accept-Encoding", "gzip");
                })
                .retrieve()
                .body(byte[].class);
        JsonNode response;
        try {
            response = objectMapper.readTree(decompress(body));
        } catch (IOException exception) {
            throw new IllegalStateException("和风天气响应解析失败", exception);
        }
        if (response == null || !"200".equals(response.path("code").asText())) {
            return List.of();
        }
        List<WeatherFact> result = new ArrayList<>();
        for (JsonNode day : response.path("daily")) {
            LocalDate date = LocalDate.parse(day.path("fxDate").asText());
            String summary = day.path("textDay").asText("天气信息暂缺");
            String temperature = day.path("tempMin").asText("-") + "~" + day.path("tempMax").asText("-") + "°C";
            boolean outdoorFriendly = BAD_WEATHER.stream().noneMatch(summary::contains);
            result.add(new WeatherFact(date, summary, temperature, outdoorFriendly));
        }
        return List.copyOf(result);
    }

    private byte[] decompress(byte[] body) throws IOException {
        if (body == null || body.length < 2 || (body[0] & 0xff) != 0x1f || (body[1] & 0xff) != 0x8b) {
            return body == null ? new byte[0] : body;
        }
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(body))) {
            return input.readAllBytes();
        }
    }
}
