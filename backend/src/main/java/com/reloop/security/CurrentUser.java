package com.reloop.security;

import com.reloop.entity.User;
import com.reloop.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    /** The authenticated user (detached entity; use its id to reload when modifying). */
    public static User get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof User u)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return u;
    }

    /** The authenticated user or null for anonymous callers (public endpoints that personalise output). */
    public static User getOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof User u) ? u : null;
    }
}
