package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.AttractionFact;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.TravelContext;
import com.travelagent.travelplanner.itinerary.application.TravelDataGateway.WeatherFact;
import com.travelagent.travelplanner.itinerary.domain.BudgetSummary;
import com.travelagent.travelplanner.itinerary.domain.DayPlan;
import com.travelagent.travelplanner.itinerary.domain.Itinerary;
import com.travelagent.travelplanner.itinerary.domain.ItineraryItem;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 基于已验证事实生成结构化行程；大模型不参与关键预算与时间计算。 */
@Component
public class ItineraryComposer {

    private static final BigDecimal LUNCH_PER_PERSON = BigDecimal.valueOf(80L);
    private static final BigDecimal DINNER_PER_PERSON = BigDecimal.valueOf(100L);
    private static final BigDecimal LODGING_AND_LOCAL_TRANSPORT_PER_PERSON_DAY = BigDecimal.valueOf(320L);

    /** 将需求和旅行事实组装为每日行程。 */
    public Itinerary compose(TripRequest request, TravelContext context) {
        Map<java.time.LocalDate, WeatherFact> weatherByDate = context.weather().stream()
                .collect(Collectors.toMap(WeatherFact::date, Function.identity(), (first, ignored) -> first));
        List<DayPlan> days = new ArrayList<>();
        BigDecimal attractionTotal = BigDecimal.ZERO;
        BigDecimal foodTotal = BigDecimal.ZERO;
        int attractionCursor = 0;

        for (int dayNumber = 1; dayNumber <= request.days(); dayNumber++) {
            String city = cityForDay(request, dayNumber);
            List<AttractionFact> cityAttractions = context.attractions().stream()
                    .filter(item -> item.city().equals(city))
                    .toList();
            if (cityAttractions.isEmpty()) {
                cityAttractions = context.attractions();
            }
            AttractionFact morning = cityAttractions.get(attractionCursor++ % cityAttractions.size());
            AttractionFact afternoon = cityAttractions.get(attractionCursor++ % cityAttractions.size());
            BigDecimal morningCost = perTraveler(morning.estimatedCost(), request.travelers());
            BigDecimal afternoonCost = perTraveler(afternoon.estimatedCost(), request.travelers());
            BigDecimal lunchCost = perTraveler(LUNCH_PER_PERSON, request.travelers());
            BigDecimal dinnerCost = perTraveler(DINNER_PER_PERSON, request.travelers());

            List<ItineraryItem> items = List.of(
                    new ItineraryItem(morning.name(), morning.category(), LocalTime.of(9, 0), LocalTime.of(11, 30),
                            morning.address(), morningCost, "已预留排队与市内交通缓冲时间"),
                    new ItineraryItem("当地特色午餐", "餐饮", LocalTime.of(12, 0), LocalTime.of(13, 30),
                            city, lunchCost, foodNote(request)),
                    new ItineraryItem(afternoon.name(), afternoon.category(), LocalTime.of(14, 0), LocalTime.of(16, 30),
                            afternoon.address(), afternoonCost, "出发前请再次确认真实开放时间"),
                    new ItineraryItem("晚餐与自由活动", "餐饮", LocalTime.of(18, 0), LocalTime.of(19, 30),
                            city, dinnerCost, "可根据体力替换为酒店附近活动"));
            attractionTotal = attractionTotal.add(morningCost).add(afternoonCost);
            foodTotal = foodTotal.add(lunchCost).add(dinnerCost);

            List<String> notes = new ArrayList<>();
            Optional<WeatherFact> weather = Optional.ofNullable(
                    weatherByDate.get(request.startDate().plusDays(dayNumber - 1L)));
            weather.ifPresent(fact -> notes.add(fact.summary() + "，" + fact.temperature()));
            if (weather.isPresent() && !weather.get().outdoorFriendly()) {
                notes.add("当天天气可能影响户外游览，建议保留室内替代方案");
            }
            days.add(new DayPlan(dayNumber, request.startDate().plusDays(dayNumber - 1L), city, items, notes));
        }

        BigDecimal accommodationAndTransport = LODGING_AND_LOCAL_TRANSPORT_PER_PERSON_DAY
                .multiply(BigDecimal.valueOf((long) request.days() * request.travelers()));
        BigDecimal total = attractionTotal.add(foodTotal).add(accommodationAndTransport);
        BudgetSummary summary = new BudgetSummary(attractionTotal, foodTotal, accommodationAndTransport, total);
        List<String> warnings = new ArrayList<>(context.warnings());
        warnings.add("门票、酒店、天气和开放时间以出行前实时查询为准");

        return new Itinerary(
                String.join("、", request.destinations()) + " " + request.days() + " 日旅行计划",
                total,
                summary,
                List.copyOf(days),
                List.copyOf(warnings),
                context.evidences());
    }

    private String cityForDay(TripRequest request, int dayNumber) {
        int destinationIndex = Math.min(request.destinations().size() - 1,
                (int) Math.floor((double) (dayNumber - 1) * request.destinations().size() / request.days()));
        return request.destinations().get(destinationIndex);
    }

    private BigDecimal perTraveler(BigDecimal amount, int travelers) {
        return amount.multiply(BigDecimal.valueOf(travelers));
    }

    private String foodNote(TripRequest request) {
        if (request.forbiddenItems().contains("辛辣食物")) {
            return "已避开辛辣食物，请向餐厅再次确认配料";
        }
        return "优先选择当地特色且交通便利的餐厅";
    }
}
