package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@AllArgsConstructor
public class UserEventEmitterBo {
    public static final String USER_MAX_ENERGY_CHANGE = "user_max_energy_change";
    public static final String USER_DATA_CHANGE = "user_data_change";

    private final SocketIoService socketIoService;
    private final ImprovementBo improvementBo;
    private final DtoUtilService dtoUtilService;
    private final UserEnergyServiceBo userEnergyServiceBo;
    private final UserStorageRepository userStorageRepository;
    private final TransactionUtilService transactionUtilService;

    @PostConstruct
    public void init() {
        improvementBo.addChangeListener(ImprovementChangeEnum.MORE_ENERGY, (userId, improvement) ->
                transactionUtilService.doAfterCommit(() ->
                        emitMaxEnergyChange(userId)
                )
        );
    }

    public void emitMaxEnergyChange(Integer userId) {
        var user = userStorageRepository.getById(userId);
        socketIoService.sendMessage(userId, USER_MAX_ENERGY_CHANGE,
                () -> userEnergyServiceBo.findMaxEnergy(user));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.7
     */
    public void emitUserData(UserStorage user) {
        socketIoService.sendMessage(user, USER_DATA_CHANGE, () -> findData(user));
    }

    /**
     * Finds all the user data as a DTO
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public UserStorageDto findData(UserStorage user) {
        var userDto = new UserStorageDto();
        userDto.dtoFromEntity(user);
        userDto.setImprovements(improvementBo.findUserImprovement(user));
        userDto.setFactionDto(dtoUtilService.dtoFromEntity(FactionDto.class, user.getFaction()));
        userDto.setHomePlanetDto(dtoUtilService.dtoFromEntity(PlanetDto.class, user.getHomePlanet()));
        userDto.setAlliance(dtoUtilService.dtoFromEntity(AllianceDto.class, user.getAlliance()));

        var galaxyData = user.getHomePlanet().getGalaxy();
        userDto.getHomePlanetDto().setGalaxyId(galaxyData.getId());
        userDto.getHomePlanetDto().setGalaxyName(galaxyData.getName());
        userDto.setConsumedEnergy(userEnergyServiceBo.findConsumedEnergy(user));
        userDto.setMaxEnergy(userEnergyServiceBo.findMaxEnergy(user));
        return userDto;
    }
}
