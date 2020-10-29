package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/speed-impact-group")
@ApplicationScope
public class SpeedImpactRestService implements SyncSource {

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

	@Override
	public Map<String, Supplier<Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("speed_impact_group_unlocked_change", this::findUnlockedIds)
				.build();
	}
}
