package com.kevinguanchedarias.owgejava.filter;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationFilter;

public class BootJwtAuthenticationFilter extends JwtAuthenticationFilter {
    public BootJwtAuthenticationFilter(String pattern) {
        super(true);
        setFilterProcessesUrl(pattern);
    }
}