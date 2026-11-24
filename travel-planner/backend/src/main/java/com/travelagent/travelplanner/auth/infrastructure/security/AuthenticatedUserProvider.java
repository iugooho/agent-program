package com.travelagent.travelplanner.auth.infrastructure.security;

import com.travelagent.travelplanner.common.error.BusinessException;
import com.travelagent.travelplanner.common.error.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 从安全上下文读取当前登录用户。 */
@Component
public class AuthenticatedUserProvider {

    /**
     * 获取当前登录用户。
     *
     * @return 当前用户
     */
    public AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return user;
    }
}
