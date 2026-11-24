package com.travelagent.travelplanner.itinerary.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 旅行计划数据权限 SQL 测试。 */
@SpringBootTest(properties = {
        "app.auth.redis.enabled=false",
        "app.email.mode=disabled",
        "app.bootstrap-admin.enabled=false"
})
@Transactional
class TravelPlanMapperTest {

    private static final String REQUEST_JSON = """
            {"departureCity":"北京","destinations":["杭州"],"startDate":"2026-11-24",
             "days":1,"travelers":1,"budget":3000,"travelStyles":[],
             "transportPreference":"高铁","foodPreferences":[],"forbiddenItems":[],
             "accommodationPreferences":[]}
            """;
    private static final String ITINERARY_JSON = """
            {"title":"测试行程","totalBudget":1000,
             "budgetSummary":{"attractions":100,"food":300,
             "accommodationAndTransport":600,"total":1000},
             "days":[],"evidences":[],"warnings":[]}
            """;

    @Autowired
    private TravelPlanMapper mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private long firstUserId;
    private long secondUserId;
    private long firstPlanId;
    private long secondPlanId;
    private long otherDepartmentPlanId;
    private long techDepartmentId;
    private long operationsDepartmentId;

    @BeforeEach
    void setUp() {
        firstUserId = insertUser("scope-first");
        secondUserId = insertUser("scope-second");
        long thirdUserId = insertUser("scope-third");
        techDepartmentId = departmentId("TECH");
        operationsDepartmentId = departmentId("OPERATIONS");

        firstPlanId = insertPlan(firstUserId, techDepartmentId);
        secondPlanId = insertPlan(secondUserId, techDepartmentId);
        otherDepartmentPlanId = insertPlan(thirdUserId, operationsDepartmentId);
    }

    @Test
    @DisplayName("仅本人范围只返回当前用户计划")
    void selfScopeReturnsOnlyOwnedPlans() {
        List<TravelPlanRow> plans = mapper.findVisible(
                firstUserId,
                false,
                true,
                Set.of());

        assertEquals(List.of(firstPlanId), ids(plans));
    }

    @Test
    @DisplayName("部门范围返回部门成员计划，并按范围合并本人计划")
    void departmentScopeFiltersResources() {
        List<TravelPlanRow> departmentPlans = mapper.findVisible(
                firstUserId,
                false,
                false,
                Set.of(techDepartmentId));
        List<TravelPlanRow> departmentAndSelfPlans = mapper.findVisible(
                firstUserId,
                false,
                true,
                Set.of(operationsDepartmentId));

        assertEquals(List.of(secondPlanId, firstPlanId), ids(departmentPlans));
        assertTrue(ids(departmentAndSelfPlans).contains(firstPlanId));
        assertTrue(ids(departmentAndSelfPlans).contains(otherDepartmentPlanId));
    }

    @Test
    @DisplayName("全部数据与无数据权限使用明确结果")
    void allDataAndEmptyScopeAreExplicit() {
        List<TravelPlanRow> allPlans = mapper.findVisible(
                firstUserId,
                true,
                false,
                Set.of());
        List<TravelPlanRow> noPlans = mapper.findVisible(
                firstUserId,
                false,
                false,
                Set.of());

        assertEquals(
                Set.of(firstPlanId, secondPlanId, otherDepartmentPlanId),
                Set.copyOf(ids(allPlans)));
        assertTrue(noPlans.isEmpty());
    }

    private long insertUser(String username) {
        jdbcTemplate.update("""
                INSERT INTO users (username, email, password_hash, nickname)
                VALUES (?, ?, '$2a$10$placeholder', ?)
                """, username, username + "@example.com", username);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username);
    }

    private long departmentId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_department WHERE code = ?",
                Long.class,
                code);
    }

    private long insertPlan(long ownerId, Long departmentId) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ownerId", ownerId);
        parameters.put("departmentId", departmentId);
        parameters.put("requestPayload", REQUEST_JSON);
        parameters.put("itineraryPayload", ITINERARY_JSON);
        parameters.put("mode", "test");
        mapper.insert(parameters);
        return ((Number) parameters.get("id")).longValue();
    }

    private List<Long> ids(List<TravelPlanRow> rows) {
        return rows.stream().map(TravelPlanRow::id).toList();
    }
}
