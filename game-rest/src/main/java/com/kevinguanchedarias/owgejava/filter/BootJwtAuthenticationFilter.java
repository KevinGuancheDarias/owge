package com.kevinguanchedarias.owgejava.filter;

public class BootJwtAuthenticationFilter extends OwgeJwtAuthenticationFilter {
	public BootJwtAuthenticationFilter(String pattern) {
		super(true);
		setFilterProcessesUrl(pattern);
	}
}