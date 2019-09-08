/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.AdminUserBo;
import com.kevinguanchedarias.owgejava.pojo.TokenPojo;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("admin")
@ApplicationScope
public class LoginRestService {

	@Autowired
	private AdminUserBo adminUserBo;

	@PostMapping(value = "login")
	public TokenPojo login() {
		return adminUserBo.login();
	}
}
