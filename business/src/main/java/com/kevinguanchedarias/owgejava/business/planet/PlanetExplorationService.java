package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanetExplorationService {
    public static final String PLANET_EXPLORED_EVENT = "planet_explored_event";
    
    private final PlanetRepository planetRepository;
    private final ExploredPlanetRepository exploredPlanetRepository;
    private final SocketIoService socketIoService;
    private final DtoUtilService dtoUtilService;

    public boolean isExplored(UserStorage user, Planet planet) {
        return isExplored(user.getId(), planet.getId());
    }

    public boolean isExplored(Integer userId, Long planetId) {
        return planetRepository.isOfUserProperty(userId, planetId)
                || exploredPlanetRepository.findOneByUserIdAndPlanetId(userId, planetId) != null;
    }

    public void defineAsExplored(UserStorage user, Planet targetPlanet) {
        var exploredPlanet = new ExploredPlanet();
        exploredPlanet.setUser(user);
        exploredPlanet.setPlanet(targetPlanet);
        exploredPlanetRepository.save(exploredPlanet);
        socketIoService.sendMessage(user, PLANET_EXPLORED_EVENT, () ->
                dtoUtilService.dtoFromEntity(PlanetDto.class, SpringRepositoryUtil.findByIdOrDie(planetRepository, targetPlanet.getId()))
        );
    }
}
