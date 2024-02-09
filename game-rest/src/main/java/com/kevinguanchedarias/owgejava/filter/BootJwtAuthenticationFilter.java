package com.kevinguanchedarias.owgejava.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BootJwtAuthenticationFilter extends OwgeJwtAuthenticationFilter {
    public BootJwtAuthenticationFilter(String pattern) {
        super(true);
        setFilterProcessesUrl(pattern);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return request.getMethod().equals("OPTIONS") ? false : super.requiresAuthentication(request, response);
    }
}