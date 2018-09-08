package com.kevinguanchedarias.sgtjava.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.UpgradeTypeBo;
import com.kevinguanchedarias.sgtjava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

@RestController
@RequestMapping("upgradeType")
@ApplicationScope
public class UpgradeTypeRestService {

	@Autowired
	private UpgradeTypeBo upgradeTypeBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public List<UpgradeTypeDto> findAll() {
		return dtoUtilService.convertEntireArray(UpgradeTypeDto.class, upgradeTypeBo.findAll());
	}
}
