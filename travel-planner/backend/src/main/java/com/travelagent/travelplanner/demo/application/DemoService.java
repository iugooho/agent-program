package com.travelagent.travelplanner.demo.application;

import org.springframework.stereotype.Service;

/**
 * Demo business service.
 */
@Service
public class DemoService {

    /**
     * Returns the demo greeting.
     *
     * @return greeting text
     */
    public String hello() {
        return "Hello";
    }
}
