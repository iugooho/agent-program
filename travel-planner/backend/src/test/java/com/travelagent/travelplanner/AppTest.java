package com.travelagent.travelplanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 脚手架自带的冒烟测试，保证 mvn test 开箱可用。 */
class AppTest {

    @Test
    @DisplayName("greeting 返回项目名")
    void greetingContainsProjectName() {
        assertTrue(App.greeting().contains("travel-planner"), "greeting 应该包含项目名");
    }
}
