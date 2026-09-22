package com.nira.finance.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to pull the authenticated user id out of the security context. */
public class CurrentUser {
    public static Long id() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }
}
