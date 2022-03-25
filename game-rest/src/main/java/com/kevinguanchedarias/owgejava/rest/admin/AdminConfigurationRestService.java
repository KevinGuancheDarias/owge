package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.dto.ConfigurationDto;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Optional;

/**
 * @author kevin
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/configuration")
public class AdminConfigurationRestService {

    @Autowired
    private ConfigurationBo configurationBo;

    @Autowired
    private DtoUtilService dtoUtilService;

    /**
     * Finds all <b>un</b>privileged configuration properties
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
     * @since 0.9.0
     */
    @GetMapping
    public List<ConfigurationDto> findAll() {
        return dtoUtilService.convertEntireArray(ConfigurationDto.class, configurationBo.findAllNonPrivileged());
    }

    @GetMapping("{name}")
    public ConfigurationDto findOne(@PathVariable String name) {
        Configuration configuration = configurationBo.findConfigurationParam(name);
        return configuration.getPrivileged() ? null
                : dtoUtilService.dtoFromEntity(ConfigurationDto.class, configuration);
    }

    @PostMapping
    public ConfigurationDto saveNew(@RequestBody ConfigurationDto configurationDto) {
        Optional<Configuration> configuration = configurationBo.findOne(configurationDto.getName());
        if (configuration.isPresent()) {
            throw new SgtBackendInvalidInputException(
                    new GameBackendErrorPojo("I18N_DUPLICATED_KEY", "Key in use", getClass()));
        } else {
            return dtoUtilService.dtoFromEntity(ConfigurationDto.class,
                    configurationBo.save(dtoUtilService.entityFromDto(Configuration.class, configurationDto)));
        }
    }

    @PutMapping("{name}")
    public ConfigurationDto saveExisting(@PathVariable String name, @RequestBody ConfigurationDto configurationDto) {
        Optional<Configuration> configuration = configurationBo.findOne(configurationDto.getName());
        if (!configuration.isPresent() || configuration.get().getPrivileged()) {
            throw NotFoundException.fromAffected(getClass(), configurationDto.getName());
        } else if (!name.equals(configurationDto.getName())) {
            throw new SgtBackendInvalidInputException(new GameBackendErrorPojo("I18N_PATH_AND_BODY_NOT_MATCH",
                    "Id field of the body and id of the path param, must match", getClass()));
        } else {
            return dtoUtilService.dtoFromEntity(ConfigurationDto.class,
                    configurationBo.save(dtoUtilService.entityFromDto(Configuration.class, configurationDto)));
        }
    }

    @DeleteMapping("{name}")
    public void delete(@PathVariable String name) {
        configurationBo.deleteOne(name);
    }
}
