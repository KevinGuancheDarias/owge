package com.kevinguanchedarias.owgejava.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Hack for Spring Boot due to lack of ability to get the context from outside
 * spring context
 * 
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
 *
 */
@Service
public class ContextHolderUtilService {
	private static ApplicationContext context;

	@Autowired
	public ContextHolderUtilService(ApplicationContext ac) {
		context = ac;
	}

	public static ApplicationContext getContext() {
		return context;
	}
}
