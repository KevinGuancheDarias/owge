package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.dto.UnitTypeDto;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;

@RestController
@RequestMapping("unitType")
@ApplicationScope
public class UnitTypeRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public List<UnitTypeDto> findAll() {
		return unitTypeBo.findAll().stream().map(current -> {
			UnitTypeDto currentDto = new UnitTypeDto();
			currentDto.dtoFromEntity(current);
			UserStorage user = userStorageBo.findLoggedInWithDetails(false);
			currentDto.setComputedMaxCount(unitTypeBo.findUniTypeLimitByUser(user, current.getId()));
			currentDto.setUserBuilt(obtainedUnitBo.countByUserAndUnitType(user, current.getId()));
			return currentDto;
		}).collect(Collectors.toList());
	}
}
