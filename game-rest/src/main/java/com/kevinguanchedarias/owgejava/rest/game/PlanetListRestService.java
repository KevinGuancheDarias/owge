package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.PlanetListBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.PlanetListDto;
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
public class PlanetListRestService {
	@Autowired
	private PlanetListBo planetListBo;

	@Autowired
	private UserStorageBo userStorageBo;

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping
	public List<PlanetListDto> findMy() {
		return planetListBo.toDto(planetListBo.findByUserId(userStorageBo.findLoggedIn().getId()));
	}

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
}
