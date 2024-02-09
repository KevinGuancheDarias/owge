package com.kevinguanchedarias.owgejava.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

public class OwgeNullSecurityContextRepository implements SecurityContextRepository {

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        return new SecurityContextImpl();
    }

    @Override
    public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        var context = SecurityContextHolder.getContext();
        return new DeferredSecurityContext() {
            @Override
            public boolean isGenerated() {
                return false;
            }

            @Override
            public SecurityContext get() {
                return context;
            }
        };
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Do nothing
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return false;
    }

}
