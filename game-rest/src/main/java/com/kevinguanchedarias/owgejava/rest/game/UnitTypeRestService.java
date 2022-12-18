package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.responses.UnitTypeResponse;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/unitType")
@ApplicationScope
@AllArgsConstructor
public class UnitTypeRestService implements SyncSource {

    private final UserStorageRepository userStorageRepository;

    private final UnitTypeBo unitTypeBo;
    private final UnitTypeRepository unitTypeRepository;

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("unit_type_change", this::loadData).build();
    }

    private List<UnitTypeResponse> loadData(UserStorage loggedUser) {
        var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, loggedUser.getId());
        return unitTypeRepository.findAll().stream().map(current -> {
            var unitTypeResponse = new UnitTypeResponse();
            current.getSpeedImpactGroup().setRequirementGroups(null);
            unitTypeResponse.dtoFromEntity(current);
            unitTypeResponse.setComputedMaxCount(unitTypeBo.findUniTypeLimitByUser(user, current));
            if (unitTypeBo.hasMaxCount(user.getFaction(), current)) {
                unitTypeResponse.setUserBuilt(unitTypeBo.countUnitsByUserAndUnitType(user, current));
            }
            unitTypeResponse.setUsed(unitTypeBo.isUsed(current.getId()));
            return unitTypeResponse;
        }).toList();
    }
}
