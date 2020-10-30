package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetListBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.pojo.PlanetListAddRequestBody;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@RequestMapping("game/planet-list")
@ApplicationScope
public class PlanetListRestService implements SyncSource {
	@Autowired
	private PlanetListBo planetListBo;

	/**
	 *
	 * @param body
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping
	public void add(@RequestBody PlanetListAddRequestBody body) {
		planetListBo.myAdd(body.getPlanetId(), body.getName());
	}

	@DeleteMapping("{planetId}")
	public void delete(@PathVariable Long planetId) {
		planetListBo.myDelete(planetId);
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("planet_user_list_change",
				user -> planetListBo.toDto(planetListBo.findByUserId(user.getId()))).build();
	}
}
