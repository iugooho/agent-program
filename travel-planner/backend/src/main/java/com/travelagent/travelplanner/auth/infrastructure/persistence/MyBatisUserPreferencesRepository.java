package com.travelagent.travelplanner.auth.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelagent.travelplanner.auth.domain.UserPreferences;
import com.travelagent.travelplanner.auth.domain.UserPreferencesRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis 的用户偏好仓储。 */
@Repository
public class MyBatisUserPreferencesRepository implements UserPreferencesRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final UserPreferencesMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * 创建用户偏好仓储。
     *
     * @param mapper MyBatis Mapper
     * @param objectMapper JSON 工具
     */
    public MyBatisUserPreferencesRepository(
            UserPreferencesMapper mapper,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<UserPreferences> findByUserId(long userId) {
        return Optional.ofNullable(mapper.findByUserId(userId)).map(this::toDomain);
    }

    @Override
    public UserPreferences save(UserPreferences preferences) {
        String forbiddenJson = writeForbiddenList(preferences.forbiddenList());
        if (mapper.update(
                preferences.userId(),
                preferences.travelStyleDefault(),
                preferences.foodHabit(),
                forbiddenJson,
                preferences.accommodationPreference(),
                preferences.budgetLevel()) == 0) {
            mapper.insert(
                    preferences.userId(),
                    preferences.travelStyleDefault(),
                    preferences.foodHabit(),
                    forbiddenJson,
                    preferences.accommodationPreference(),
                    preferences.budgetLevel());
        }
        return findByUserId(preferences.userId())
                .orElseThrow(() -> new IllegalStateException("用户偏好保存后未能查询到"));
    }

    private UserPreferences toDomain(UserPreferencesRow row) {
        return new UserPreferences(
                row.userId(),
                row.travelStyleDefault(),
                row.foodHabit(),
                readForbiddenList(row.forbiddenList()),
                row.accommodationPreference(),
                row.budgetLevel(),
                row.updateTime());
    }

    private String writeForbiddenList(List<String> forbiddenList) {
        try {
            return objectMapper.writeValueAsString(forbiddenList);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化禁忌清单", exception);
        }
    }

    private List<String> readForbiddenList(String forbiddenList) {
        if (forbiddenList == null || forbiddenList.isBlank()) {
            return List.of();
        }
        try {
            String normalized = forbiddenList.strip();
            if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
                normalized = objectMapper.readValue(normalized, String.class);
            }
            return objectMapper.readValue(normalized, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法解析禁忌清单", exception);
        }
    }
}
