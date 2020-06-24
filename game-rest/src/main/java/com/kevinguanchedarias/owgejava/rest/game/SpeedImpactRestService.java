package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/speed-impact-group")
@ApplicationScope
public class SpeedImpactRestService {

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("unlocked-ids")
	public List<Integer> findUnlockedIds() {
		return speedImpactGroupBo.findCrossGalaxyUnlocked(userStorageBo.findLoggedIn());
	}
}
