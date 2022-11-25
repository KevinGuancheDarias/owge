package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.PlanetListDto;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.PlanetList;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.embeddedid.PlanetUser;
import com.kevinguanchedarias.owgejava.repository.PlanetListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanetListBo implements WithToDtoTrait<PlanetList, PlanetListDto> {

    @Autowired
    private PlanetListRepository repository;

    @Autowired
    private SocketIoService socketIoService;

    @Autowired
    private UserStorageBo userStorageBo;

    @Autowired
    private PlanetBo planetBo;

    @Override
    public Class<PlanetListDto> getDtoClass() {
        return PlanetListDto.class;
    }

    /**
     * Handles unexplored, if it's unexplored planet
     *
     * @param userId
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<PlanetList> findByUserId(Integer userId) {
        List<PlanetList> retVal = repository.findByPlanetUserUserId(userId);
        planetBo.cleanUpUnexplored(userId,
                retVal.stream().map(current -> current.getPlanetUser().getPlanet()).collect(Collectors.toList()));
        return retVal;
    }

    /**
     * Adds a planet to the list and emits the value
     *
     * @param planetId
     * @param name
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void myAdd(Long planetId, String name) {
        UserStorage user = userStorageBo.findLoggedInWithDetails();
        PlanetList planetList = new PlanetList();
        planetList.setPlanetUser(new PlanetUser(user, planetBo.findByIdOrDie(planetId)));
        planetList.setName(name);
        repository.save(planetList);
        emitChangeToUser(user);

    }

    /**
     * @param planetId
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void myDelete(Long planetId) {
        UserStorage user = userStorageBo.findLoggedInWithDetails();
        repository.deleteById(new PlanetUser(user, planetBo.findByIdOrDie(planetId)));
        emitChangeToUser(user);
    }

    /**
     * Emits to all users that has the specified planet in the list
     *
     * @param planet
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
        socketIoService.sendMessage(userId, "planet_user_list_change", () -> toDto(findByUserId(userId)));
    }
}
