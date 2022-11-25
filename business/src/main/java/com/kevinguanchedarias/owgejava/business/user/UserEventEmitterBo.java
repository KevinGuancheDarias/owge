package com.kevinguanchedarias.owgejava.business.user;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.dto.AllianceDto;
import com.kevinguanchedarias.owgejava.dto.FactionDto;
import com.kevinguanchedarias.owgejava.dto.PlanetDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserEventEmitterBo {
    private final SocketIoService socketIoService;
    private final ImprovementBo improvementBo;
    private final DtoUtilService dtoUtilService;
    private final UserEnergyServiceBo userEnergyServiceBo;
    private final UserStorageRepository userStorageRepository;

    public void emitMaxEnergyChange(Integer userId) {
        var user = userStorageRepository.getById(userId);
        socketIoService.sendMessage(userId, "user_max_energy_change",
                () -> userEnergyServiceBo.findMaxEnergy(user));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.7
     */
    public void emitUserData(UserStorage user) {
        socketIoService.sendMessage(user, "user_data_change", () -> findData(user));
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
