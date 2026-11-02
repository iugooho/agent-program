package com.travelagent.travelplanner.itinerary.application;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import com.travelagent.travelplanner.itinerary.domain.TripRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** 无需模型即可运行的规则解析器，同时作为大模型不可用时的降级路径。 */
@Component
public class RuleBasedRequirementParser {

    private static final Pattern DAYS = Pattern.compile("([0-9]{1,2})\\s*(?:天|日)");
    private static final Pattern TRAVELERS = Pattern.compile("([0-9]{1,2})\\s*(?:人|位)");
    private static final Pattern BUDGET = Pattern.compile(
            "(?:预算|花费|费用)[^0-9]{0,8}([0-9]+(?:\\.[0-9]+)?)\\s*(万|千)?");
    private static final Pattern DEPARTURE = Pattern.compile("从([^，,。；;\\s]{2,12}?)(?:出发|去|到)");
    private static final Pattern MONTH_DAY = Pattern.compile("([0-9]{1,2})月([0-9]{1,2})日");

    /** 从一条中文需求中提取第一版规划所需字段。 */
    public TripRequest parse(String message) {
        String normalized = message == null ? "" : message.trim();
        List<String> destinations = extractDestinations(normalized);
        if (destinations.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "请提供目的地，例如：从北京去杭州玩 3 天");
        }
        int days = matchNumber(DAYS, normalized, 3);
        int travelers = matchNumber(TRAVELERS, normalized, 1);
        BigDecimal budget = extractBudget(normalized, days, travelers);
        String departure = extractDeparture(normalized);
        return new TripRequest(
                departure,
                destinations,
                extractStartDate(normalized),
                days,
                travelers,
                budget,
                extractStyles(normalized),
                normalized.contains("自驾") ? "自驾" : "高铁或飞机",
                extractFoodPreferences(normalized),
                extractForbiddenItems(normalized),
                normalized.contains("民宿") ? List.of("民宿") : List.of("交通便利"));
    }

    private List<String> extractDestinations(String message) {
        for (String marker : List.of("去", "到", "前往")) {
            int start = message.indexOf(marker);
            if (start < 0 || start + marker.length() >= message.length()) {
                continue;
            }
            String candidate = message.substring(start + marker.length())
                    .split("[，,。；;！!？?\\s]|玩|旅游|旅行|度假|安排|待|住", 2)[0]
                    .trim();
            if (!candidate.isBlank() && candidate.length() <= 20) {
                return Arrays.stream(candidate.split("和|、"))
                        .map(String::trim)
                        .filter(city -> !city.isBlank())
                        .distinct()
                        .toList();
            }
        }
        return List.of();
    }

    private String extractDeparture(String message) {
        Matcher matcher = DEPARTURE.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "北京";
    }

    private int matchNumber(Pattern pattern, String message, int fallback) {
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private BigDecimal extractBudget(String message, int days, int travelers) {
        Matcher matcher = BUDGET.matcher(message);
        if (!matcher.find()) {
            return BigDecimal.valueOf((long) days * travelers * 900L);
        }
        BigDecimal amount = new BigDecimal(matcher.group(1));
        if ("万".equals(matcher.group(2))) {
            amount = amount.multiply(BigDecimal.valueOf(10000L));
        } else if ("千".equals(matcher.group(2))) {
            amount = amount.multiply(BigDecimal.valueOf(1000L));
        }
        return amount;
    }

    private LocalDate extractStartDate(String message) {
        if (message.contains("后天")) {
            return LocalDate.now().plusDays(2);
        }
        if (message.contains("明天")) {
            return LocalDate.now().plusDays(1);
        }
        Matcher matcher = MONTH_DAY.matcher(message);
        if (matcher.find()) {
            int month = Integer.parseInt(matcher.group(1));
            int day = Integer.parseInt(matcher.group(2));
            LocalDate candidate = LocalDate.of(LocalDate.now().getYear(), month, day);
            return candidate.isBefore(LocalDate.now()) ? candidate.plusYears(1) : candidate;
        }
        return LocalDate.now().plusDays(7);
    }

    private List<String> extractStyles(String message) {
        List<String> styles = new ArrayList<>();
        addIfPresent(styles, message, "亲子", "亲子");
        addIfPresent(styles, message, "情侣", "情侣");
        addIfPresent(styles, message, "浪漫", "情侣");
        addIfPresent(styles, message, "休闲", "休闲");
        addIfPresent(styles, message, "慢游", "休闲");
        addIfPresent(styles, message, "轻松", "休闲");
        addIfPresent(styles, message, "徒步", "户外");
        addIfPresent(styles, message, "摄影", "摄影");
        addIfPresent(styles, message, "美食", "美食");
        if (styles.isEmpty()) {
            styles.add("经典观光");
        }
        return List.copyOf(styles);
    }

    private void addIfPresent(List<String> target, String message, String keyword, String value) {
        if (message.contains(keyword) && !target.contains(value)) {
            target.add(value);
        }
    }

    private List<String> extractFoodPreferences(String message) {
        List<String> result = new ArrayList<>();
        addIfPresent(result, message, "吃辣", "吃辣");
        addIfPresent(result, message, "素食", "素食");
        addIfPresent(result, message, "美食", "当地特色");
        return List.copyOf(result);
    }

    private List<String> extractForbiddenItems(String message) {
        List<String> result = new ArrayList<>();
        addIfPresent(result, message, "不要购物", "购物");
        addIfPresent(result, message, "不购物", "购物");
        addIfPresent(result, message, "不吃辣", "辛辣食物");
        addIfPresent(result, message, "少走路", "高强度步行");
        return List.copyOf(result);
    }
}
