package com.travelagent.travelplanner;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 脚手架自带的冒烟测试，保证 mvn test 开箱可用。 */
class AppTest {

    @Test
    @DisplayName("greeting 返回非空欢迎语")
    void greetingIsNotBlank() {
        assertFalse(App.greeting().isBlank(), "greeting 不应为空");
    }
}
