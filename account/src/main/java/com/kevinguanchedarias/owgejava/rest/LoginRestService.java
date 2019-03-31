package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.UserBo;
import com.kevinguanchedarias.owgejava.entity.User;

@RestController
@ApplicationScope
public class LoginRestService {

	@Autowired
	private UserBo userBo;

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public Object login(@RequestParam("email") String email, @RequestParam("password") String password) {
		return userBo.login(email, password);
	}

	@RequestMapping(value = "/register", method = RequestMethod.POST, consumes = "application/json")
	public Object register(@RequestBody User user) {
		user.setId(null);
		user.setActivated(false);
		userBo.register(user);

		return user;
	}
}
