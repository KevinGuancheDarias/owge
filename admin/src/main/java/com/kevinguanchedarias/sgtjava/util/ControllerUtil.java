package com.kevinguanchedarias.sgtjava.util;

import javax.faces.context.FacesContext;

import org.springframework.web.jsf.FacesContextUtils;

public final class ControllerUtil {

	private ControllerUtil(){
		throw new AssertionError();
	}
	
	/**
	 * Allows using Autowired annotation inside  a ManagedBean
	 * @param controllerInstance
	 * @author Kevin Guanche Darias
	 */
	public static void enableAutowire(Object controllerInstance){
		FacesContextUtils.getRequiredWebApplicationContext(FacesContext.getCurrentInstance())
		.getAutowireCapableBeanFactory().autowireBean(controllerInstance);
	}
}
