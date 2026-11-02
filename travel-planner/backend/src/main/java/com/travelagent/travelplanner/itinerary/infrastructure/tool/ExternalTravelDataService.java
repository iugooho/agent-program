package com.travelagent.travelplanner.itinerary.infrastructure.tool;

import com.travelagent.travelplanner.itinerary.application.TravelDataGateway;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.AttractionFact;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.WeatherFact;
import com.travelagent.travelplanner.itinerary.domain.ToolEvidence;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 组合高德与和风数据，并在第三方故障时提供可运行的降级事实。 */
@Component
public class ExternalTravelDataService implements TravelDataGateway {

    private static final Logger log = LoggerFactory.getLogger(ExternalTravelDataService.class);
    private static final Map<String, List<String>> DEMO_ATTRACTIONS = demoAttractions();

    private final AmapClient amapClient;
    private final QWeatherClient weatherClient;

    /** 创建旅行数据服务。 */
    public ExternalTravelDataService(AmapClient amapClient, QWeatherClient weatherClient) {
        this.amapClient = amapClient;
        this.weatherClient = weatherClient;
    }

    @Override
    public TravelContext collect(TripRequest request) {
        List<AttractionFact> attractions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<ToolEvidence> evidences = new ArrayList<>();
        Map<String, String> locations = new LinkedHashMap<>();

        for (String city : request.destinations()) {
            List<AttractionFact> cityAttractions = safelyFindAttractions(city, warnings);
            attractions.addAll(cityAttractions);
            safelyLocate(city).ifPresent(location -> locations.put(city, location));
            if (!cityAttractions.isEmpty() && safelyUsesAmap(cityAttractions)) {
                evidences.add(new ToolEvidence("高德地图", city + "景点 POI", Instant.now()));
            }
        }
        if (attractions.isEmpty()) {
            String city = request.destinations().get(0);
            attractions.addAll(demoAttractions(city));
            warnings.add("未取得实时景点信息，已使用演示数据");
        }

        List<WeatherFact> weather = List.of();
        String firstCity = request.destinations().get(0);
        Optional<String> location = Optional.ofNullable(locations.get(firstCity));
        if (location.isPresent()) {
            try {
                weather = weatherClient.forecast(location.get());
                if (!weather.isEmpty()) {
                    evidences.add(new ToolEvidence("和风天气", firstCity + "未来七天预报", Instant.now()));
                }
            } catch (RuntimeException exception) {
                log.warn("和风天气查询失败 city={} error={} message={}", firstCity,
                        exception.getClass().getSimpleName(), exception.getMessage());
            }
        }
        if (weather.isEmpty()) {
            warnings.add("未取得实时天气，行程中的天气提醒暂不可用");
        }
        if (request.startDate().isAfter(java.time.LocalDate.now().plusDays(7))) {
            warnings.add("出行日期超出未来七天天气预报范围，请在临近出行时复查天气");
        }
        return new TravelContext(List.copyOf(attractions), weather, List.copyOf(evidences), List.copyOf(warnings));
    }

    private List<AttractionFact> safelyFindAttractions(String city, List<String> warnings) {
        try {
            List<AttractionFact> result = amapClient.findAttractions(city);
            if (!result.isEmpty()) {
                return result;
            }
        } catch (RuntimeException exception) {
            log.warn("高德景点查询失败 city={} error={}", city, exception.getClass().getSimpleName());
        }
        warnings.add(city + "实时景点查询不可用，已使用演示数据");
        return demoAttractions(city);
    }

    private Optional<String> safelyLocate(String city) {
        try {
            return amapClient.locate(city);
        } catch (RuntimeException exception) {
            log.warn("高德地理编码失败 city={} error={}", city, exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private boolean safelyUsesAmap(List<AttractionFact> attractions) {
        return attractions.stream().noneMatch(item -> item.address().equals(item.city()));
    }

    private List<AttractionFact> demoAttractions(String city) {
        List<String> names = DEMO_ATTRACTIONS.getOrDefault(city,
                List.of("城市地标", "城市博物馆", "老城区", "城市公园"));
        List<AttractionFact> result = new ArrayList<>();
        for (int index = 0; index < names.size(); index++) {
            result.add(new AttractionFact(city, names.get(index), "景点", city,
                    BigDecimal.valueOf(40L + index * 20L)));
        }
        return List.copyOf(result);
    }

    private static Map<String, List<String>> demoAttractions() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("杭州", List.of("西湖", "灵隐寺", "西溪国家湿地公园", "河坊街", "浙江省博物馆"));
        values.put("北京", List.of("故宫博物院", "天坛公园", "颐和园", "中国国家博物馆", "什刹海"));
        values.put("上海", List.of("外滩", "上海博物馆", "豫园", "武康路", "上海科技馆"));
        values.put("成都", List.of("成都大熊猫繁育研究基地", "武侯祠", "宽窄巷子", "杜甫草堂", "人民公园"));
        values.put("西安", List.of("秦始皇帝陵博物院", "西安城墙", "陕西历史博物馆", "大雁塔", "大唐不夜城"));
        values.put("三亚", List.of("亚龙湾", "蜈支洲岛", "天涯海角", "南山文化旅游区", "鹿回头风景区"));
        values.put("丽江", List.of("丽江古城", "玉龙雪山", "束河古镇", "黑龙潭公园", "白沙古镇"));
        return Map.copyOf(values);
    }
}
