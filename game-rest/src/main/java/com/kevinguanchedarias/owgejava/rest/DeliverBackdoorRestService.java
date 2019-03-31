package com.kevinguanchedarias.owgejava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

@RestController
@RequestMapping("deliverBackdoor")
@ApplicationScope
public class DeliverBackdoorRestService {

	@Autowired
	private SocketIoService socketIoService;

	@RequestMapping(value = "pingUser", method = RequestMethod.GET)
	public boolean pingUser(@RequestParam("targetUser") Integer targetUserId) {
		UserStorage targetUser = new UserStorage();
		targetUser.setId(targetUserId);
		socketIoService.sendMessage(targetUser, "ping", "Hello World");
		return true;
	}
}
