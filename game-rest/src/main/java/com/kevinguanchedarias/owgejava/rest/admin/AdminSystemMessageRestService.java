package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.SystemMessageBo;
import com.kevinguanchedarias.owgejava.dto.SystemMessageDto;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/system-message")
public class AdminSystemMessageRestService {
	@Autowired
	private SystemMessageBo systemMessageBo;

	/**
	 *
	 * @param message
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping
	public SystemMessageDto create(@RequestBody SystemMessageDto message) {
		return systemMessageBo.toDto(systemMessageBo.save(message));
	}
}
