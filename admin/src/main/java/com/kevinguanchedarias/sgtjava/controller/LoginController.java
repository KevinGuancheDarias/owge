package com.kevinguanchedarias.sgtjava.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.kevinsuite.commons.jsf.controller.SpringLoginController;

@Component("someComponent")
@Scope("request")
public class LoginController extends SpringLoginController {

	private static final long serialVersionUID = -1638461278239316367L;
	
}
