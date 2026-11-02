package com.travelagent.travelplanner.itinerary.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.AttractionFact;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 高德 Web 服务适配器：地理编码与景点 POI 搜索。 */
@Component
public class AmapClient {

    private final RestClient restClient;
    private final String apiKey;

    /** 创建高德客户端。 */
    public AmapClient(
            @Value("${app.travel.amap.base-url:https://restapi.amap.com}") String baseUrl,
            @Value("${app.travel.amap.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(8));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
    }

    /** 查询城市中心经纬度，格式为 lng,lat。 */
    public Optional<String> locate(String city) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v3/geocode/geo")
                        .queryParam("key", apiKey)
                        .queryParam("address", city)
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !"1".equals(response.path("status").asText())) {
            return Optional.empty();
        }
        String location = response.path("geocodes").path(0).path("location").asText("");
        return location.isBlank() ? Optional.empty() : Optional.of(location);
    }

    /** 搜索城市内的主要景点。 */
    public List<AttractionFact> findAttractions(String city) {
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v3/place/text")
                        .queryParam("key", apiKey)
                        .queryParam("keywords", "景点")
                        .queryParam("city", city)
                        .queryParam("types", "110000")
                        .queryParam("offset", 10)
                        .queryParam("page", 1)
                        .queryParam("extensions", "all")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !"1".equals(response.path("status").asText())) {
            return List.of();
        }
        List<AttractionFact> result = new ArrayList<>();
        int index = 0;
        for (JsonNode poi : response.path("pois")) {
            String name = poi.path("name").asText("").trim();
            if (name.isBlank()) {
                continue;
            }
            String address = poi.path("address").asText(city);
            String type = poi.path("type").asText("景点");
            BigDecimal estimatedCost = BigDecimal.valueOf(50L + (index % 3) * 30L);
            result.add(new AttractionFact(city, name, shortCategory(type), address, estimatedCost));
            index++;
        }
        return List.copyOf(result);
    }

    private String shortCategory(String type) {
        String[] parts = type.split(";");
        return parts.length == 0 || parts[parts.length - 1].isBlank() ? "景点" : parts[parts.length - 1];
    }
}
