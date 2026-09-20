package com.travelagent.travelplanner.demo.api;

import com.travelagent.travelplanner.api.ApiResponse;
import com.travelagent.travelplanner.demo.application.DemoService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for the demo module.
 */
@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoService demoService;

    /**
     * Creates the demo controller.
     *
     * @param demoService demo business service
     */
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /**
     * Returns a greeting.
     *
     * @return unified API response containing the greeting
     */
    @GetMapping("/hello")
    public ApiResponse<String> hello() {
        return ApiResponse.ok(demoService.hello());
    }
}
