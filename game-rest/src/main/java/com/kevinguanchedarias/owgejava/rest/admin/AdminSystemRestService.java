package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.SocketIoService;

/**
 * Has system wide actions
 *
 * @since 0.9.8
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("admin/system")
@ApplicationScope
public class AdminSystemRestService {

	@Autowired
	private SocketIoService socketIoService;

	@PostMapping("notify-updated-version")
	public void notifyUpdatedVersion() {
		socketIoService.sendMessage(0, "frontend_version_change", () -> "check for changes!!!!");
	}
}
