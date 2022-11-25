package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.business.AuditBo;
import com.kevinguanchedarias.owgejava.business.GalaxyBo;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.GalaxyDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.pojo.NavigationPojo;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

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

    @Autowired
    private AuditBo auditBo;

    @Autowired
    private UserStorageBo userStorageBo;

    @GetMapping("navigate")
    @Transactional
    public NavigationPojo navigate(@RequestParam("galaxyId") Integer galaxyId, @RequestParam("sector") Long sector,
                                   @RequestParam("quadrant") Long quadrant) {
        var retVal = new NavigationPojo();
        auditBo.doAudit(AuditActionEnum.BROWSE_COORDINATES, galaxyBo.coordinatesToString(galaxyId, sector, quadrant), null);
        retVal.setGalaxies(dtoUtilService.convertEntireArray(GalaxyDto.class, galaxyBo.findAll()));
        var planets = dtoUtilService.convertEntireArray(PlanetDto.class,
                planetBo.findByGalaxyAndSectorAndQuadrant(galaxyId, sector, quadrant));
        var userId = userStorageBo.findLoggedIn().getId();
        planets.forEach(planet -> planetBo.cleanUpUnexplored(userId, planet));
        retVal.setPlanets(planets);
        return retVal;
    }
}
