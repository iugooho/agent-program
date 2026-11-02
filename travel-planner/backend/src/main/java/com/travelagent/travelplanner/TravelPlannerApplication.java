package com.travelagent.travelplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 旅游规划智能体系统的 Spring Boot Web 应用入口。
 *
 * <p>业务代码在 com.travelagent.travelplanner 下按领域拆分包，入口类只负责启动应用。</p>
 */
@SpringBootApplication
public class TravelPlannerApplication {

    private static final Logger log = LoggerFactory.getLogger(TravelPlannerApplication.class);

    /**
     * 程序入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TravelPlannerApplication.class, args);
        log.info(greeting());
    }

    /**
     * 返回欢迎语，作为冒烟测试的断言目标。
     *
     * @return 欢迎语
     */
    public static String greeting() {
        return "Hello ";
    }
}
