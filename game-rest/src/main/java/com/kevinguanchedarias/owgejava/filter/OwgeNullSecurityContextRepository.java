package com.kevinguanchedarias.owgejava.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

public class OwgeNullSecurityContextRepository implements SecurityContextRepository {

	@Override
	public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
		return SecurityContextHolder.getContext();
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
