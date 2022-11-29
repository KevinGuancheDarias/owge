package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.planet.PlanetCleanerService;
import com.kevinguanchedarias.owgejava.dto.PlanetListDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import com.kevinguanchedarias.owgejava.repository.PlanetListRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class PlanetListBo implements WithToDtoTrait<PlanetList, PlanetListDto> {

    private final PlanetRepository planetRepository;
    private final PlanetListRepository repository;
    private final SocketIoService socketIoService;
    private final UserStorageBo userStorageBo;
    private final PlanetCleanerService planetCleanerService;
    private final DtoUtilService dtoUtilService;


    @Override
    public Class<PlanetListDto> getDtoClass() {
        return PlanetListDto.class;
    }

    /**
     * Handles unexplored, if it's unexplored planet
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<PlanetListDto> findByUserId(Integer userId) {
        var retVal = dtoUtilService.convertEntireArray(PlanetListDto.class, repository.findByPlanetUserUserId(userId));
        retVal.stream()
                .map(PlanetListDto::getPlanet)
                .forEach(planet -> planetCleanerService.cleanUpUnexplored(userId, planet));
        return retVal;
    }

    /**
     * Adds a planet to the list and emits the value
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void myAdd(Long planetId, String name) {
        UserStorage user = userStorageBo.findLoggedInWithDetails();
        PlanetList planetList = new PlanetList();
        planetList.setPlanetUser(new PlanetUser(user, SpringRepositoryUtil.findByIdOrDie(planetRepository, planetId)));
        planetList.setName(name);
        repository.save(planetList);
        emitChangeToUser(user);

    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void myDelete(Long planetId) {
        var user = userStorageBo.findLoggedInWithReference();
        repository.deleteById(new PlanetUser(user, planetRepository.getById(planetId)));
        emitChangeToUser(user);
    }

    /**
     * Emits to all users that has the specified planet in the list
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.8
     */
    public void emitByChangedPlanet(Planet planet) {
        repository.findUserIdByPlanetListPlanet(planet).forEach(this::emitChangeToUser);
    }

    private void emitChangeToUser(UserStorage user) {
        emitChangeToUser(user.getId());
    }

    private void emitChangeToUser(Integer userId) {
        socketIoService.sendMessage(userId, "planet_user_list_change", () -> findByUserId(userId));
    }
}
