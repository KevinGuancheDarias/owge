package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

@RestController
@RequestMapping("game/unitType")
@ApplicationScope
public class UnitTypeRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@GetMapping("/")
	public List<UnitTypeDto> findAll() {
		return unitTypeBo.findAll().stream().map(current -> {
			UnitTypeDto currentDto = new UnitTypeDto();
			currentDto.dtoFromEntity(current);
			UserStorage user = userStorageBo.findLoggedInWithDetails();
			currentDto.setComputedMaxCount(unitTypeBo.findUniTypeLimitByUser(user, current));
			if (current.hasMaxCount()) {
				currentDto.setUserBuilt(obtainedUnitBo.countByUserAndUnitType(user, current));
			}
			return currentDto;
		}).collect(Collectors.toList());
	}
}
