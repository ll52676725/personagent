package com.example.agentplatform.common.security;

import com.example.agentplatform.common.exception.UnauthorizedException;
import com.example.agentplatform.platform.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new UnauthorizedException("未登录");
    }
}
