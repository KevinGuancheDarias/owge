package com.kevinguanchedarias.owgejava.rest.open;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.dto.ConfigurationDto;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.7.4
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@RequestMapping("open/configuration")
@ApplicationScope
public class ConfigurationRestService {
	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@GetMapping
	public List<ConfigurationDto> findUnprivilege() {
		List<Configuration> configurations = new ArrayList<>();
		configurations.addAll(configurationBo.findAllNonPrivileged());
		configurations.addAll(configurationBo.findPrivilegedReadOnly());
		return dtoUtilService.convertEntireArray(ConfigurationDto.class, configurations);
	}
}
