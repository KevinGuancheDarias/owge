package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.entity.UnitType;

@RestController
@RequestMapping("unitType")
@ApplicationScope
public class UnitTypeRestService {

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public List<UnitType> findAll() {
		return unitTypeBo.findAll().stream().map(current -> {
			current.setChildren(null);
			current.setUpgradeEnhancements(null);
			current.setComputedMaxCount(
					unitTypeBo.findUniTypeLimitByUser(userStorageBo.findLoggedInWithDetails(false), current.getId()));
			return current;
		}).collect(Collectors.toList());
	}
}
