package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

@RestController
@RequestMapping("game/unitType")
@ApplicationScope
public class UnitTypeRestService implements SyncSource {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("unit_type_change", this::loadData).build();
	}

	private List<UnitTypeDto> loadData(UserStorage loggedUser) {
		return unitTypeBo.findAll().stream().map(current -> {
			UnitTypeDto currentDto = new UnitTypeDto();
			current.getSpeedImpactGroup().setRequirementGroups(null);
			current.setAttackRule(null);
			currentDto.dtoFromEntity(current);
			UserStorage user = userStorageBo.findById(loggedUser.getId());
			currentDto.setComputedMaxCount(unitTypeBo.findUniTypeLimitByUser(user, current));
			if (current.hasMaxCount()) {
				currentDto.setUserBuilt(obtainedUnitBo.countByUserAndUnitType(user, current));
			}
			return currentDto;
		}).collect(Collectors.toList());
	}
}
