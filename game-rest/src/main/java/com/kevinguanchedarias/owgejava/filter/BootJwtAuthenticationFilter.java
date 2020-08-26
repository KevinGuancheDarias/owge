package com.kevinguanchedarias.owgejava.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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