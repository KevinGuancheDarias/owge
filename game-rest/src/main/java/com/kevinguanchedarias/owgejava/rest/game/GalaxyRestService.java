package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.pojo.NavigationPojo;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

@RestController
@RequestMapping("game/galaxy")
@ApplicationScope
public class GalaxyRestService {

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@GetMapping("navigate")
	public NavigationPojo navigate(@RequestParam("galaxyId") Integer galaxyId, @RequestParam("sector") Long sector,
			@RequestParam("quadrant") Long quadrant) {

		final NavigationPojo retVal = new NavigationPojo();
		retVal.setGalaxies(dtoUtilService.convertEntireArray(GalaxyDto.class, galaxyBo.findAll()));
		retVal.setPlanets(dtoUtilService.convertEntireArray(PlanetDto.class,
				cleanUpUnexplored(planetBo.findByGalaxyAndSectorAndQuadrant(galaxyId, sector, quadrant))));
		return retVal;
	}

	private List<Planet> cleanUpUnexplored(List<Planet> planets) {
		planets.forEach(current -> {
			if (!planetBo.myIsExplored(current)) {
				current.setName(null);
				current.setRichness(null);
				current.setHome(null);
				current.setOwner(null);
				current.setSpecialLocation(null);
			}
		});
		return planets;
	}
}
