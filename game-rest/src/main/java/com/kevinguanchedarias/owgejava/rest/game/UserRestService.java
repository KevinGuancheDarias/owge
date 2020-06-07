package com.kevinguanchedarias.owgejava.rest.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;

@RestController
@RequestMapping("game/user")
@ApplicationScope
public class UserRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ImprovementBo improvementBo;

	@RequestMapping(value = "exists", method = RequestMethod.GET)
	public Object exists() {
		return userStorageBo.exists(userStorageBo.findLoggedIn().getId());
	}

	/**
	 * Will subscribe the user to this universe
	 *
	 * @return If everything well ok, returns true
	 * @author Kevin Guanche Darias
	 */
	@RequestMapping(value = "subscribe", method = RequestMethod.GET)
	public Object subscribe(@RequestParam("factionId") Integer factionId) {
		return userStorageBo.subscribe(factionId);
	}

	@GetMapping("findData")
	public Object findData() {
		return userStorageBo.findData(userStorageBo.findLoggedInWithDetails());
	}

	@GetMapping("improvements")
	public GroupedImprovement findImprovements() {
		return improvementBo.findUserImprovement(userStorageBo.findLoggedInWithDetails(false));
	}

}
