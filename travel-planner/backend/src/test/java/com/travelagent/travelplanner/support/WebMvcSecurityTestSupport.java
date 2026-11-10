package com.travelagent.travelplanner.support;

import com.travelagent.travelplanner.auth.application.TokenService;
import com.travelagent.travelplanner.auth.domain.SessionRevocationStore;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 为 Web MVC 切片测试提供安全过滤器所需的协作者替身。 */
public abstract class WebMvcSecurityTestSupport {

    @MockitoBean
    protected TokenService tokenService;

    @MockitoBean
    protected SessionRevocationStore sessionRevocationStore;
}
