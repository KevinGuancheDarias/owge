package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.AuditBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetCleanerService;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.pojo.NavigationPojo;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

@RestController
@RequestMapping("game/galaxy")
@ApplicationScope
@AllArgsConstructor
public class GalaxyRestService {

    private final GalaxyBo galaxyBo;
    private final PlanetBo planetBo;
    private final PlanetCleanerService planetCleanerService;
    private final DtoUtilService dtoUtilService;
    private final AuditBo auditBo;
    private final UserSessionService userSessionService;

    @GetMapping("navigate")
    @Transactional
    public NavigationPojo navigate(@RequestParam("galaxyId") Integer galaxyId, @RequestParam("sector") Long sector,
                                   @RequestParam("quadrant") Long quadrant) {
        var retVal = new NavigationPojo();
        auditBo.doAudit(AuditActionEnum.BROWSE_COORDINATES, galaxyBo.coordinatesToString(galaxyId, sector, quadrant), null);
        retVal.setGalaxies(dtoUtilService.convertEntireArray(GalaxyDto.class, galaxyBo.findAll()));
        var planets = dtoUtilService.convertEntireArray(PlanetDto.class,
                planetBo.findByGalaxyAndSectorAndQuadrant(galaxyId, sector, quadrant));
        var userId = userSessionService.findLoggedIn().getId();
        planets.forEach(planet -> planetCleanerService.cleanUpUnexplored(userId, planet));
        retVal.setPlanets(planets);
        return retVal;
    }
}
