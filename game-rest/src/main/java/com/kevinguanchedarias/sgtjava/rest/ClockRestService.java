/**
 * 
 */
package com.kevinguanchedarias.sgtjava.rest;

import java.util.Date;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

/**
 * Used to sync the browser and the server time
 *
 * @since 0.7.3
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("clock")
@ApplicationScope
public class ClockRestService {

	/**
	 * Returns the current time of the server
	 * 
	 * @return
	 * @since 0.7.3
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping()
	public Date currentTime() {
		return new Date();
	}
}
