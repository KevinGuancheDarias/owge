package com.kevinguanchedarias.owgejava.rest.game;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

@RestController
@RequestMapping("game/upgradeType")
@ApplicationScope
public class UpgradeTypeRestService implements SyncSource {

	@Autowired
	private UpgradeTypeBo upgradeTypeBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@GetMapping("/")
	public List<UpgradeTypeDto> findAll() {
		return dtoUtilService.convertEntireArray(UpgradeTypeDto.class, upgradeTypeBo.findAll());
	}

	@Override
	public Map<String, Supplier<Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("upgrade_types_change", this::findAll).build();
	}

}
