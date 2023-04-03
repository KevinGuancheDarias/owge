/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/upgrade")
public class AdminUpgradeRestService implements CrudWithFullRestService<Integer, Upgrade, UpgradeRepository, UpgradeDto>,
        WithImageRestServiceTrait<Integer, Upgrade, UpgradeDto, UpgradeRepository> {

    @Autowired
    private UpgradeRepository upgradeBo;

    @Autowired
    private UpgradeTypeBo upgradeTypeBo;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private ExceptionUtilService exceptionUtilService;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Integer, Upgrade, UpgradeRepository, UpgradeDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, Upgrade, UpgradeRepository, UpgradeDto> builder = RestCrudConfigBuilder.create();
        return builder.withBeanFactory(beanFactory).withRepository(upgradeBo).withDtoClass(UpgradeDto.class)
                .withEntityClass(Upgrade.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    @Override
    public Optional<UpgradeDto> beforeConversion(UpgradeDto dto) {
        if (dto.getClonedImprovements() == null) {
            dto.setClonedImprovements(false);
        }
        if (dto.getLevelEffect() == null) {
            dto.setLevelEffect(0.5F);
        }
        if (dto.getTime() == null || dto.getTime() < 5L) {
            dto.setTime(60L);
        }
        return CrudWithFullRestService.super.beforeConversion(dto);
    }

    @Override
    public Optional<Upgrade> beforeSave(UpgradeDto parsedDto, Upgrade entity) {
        if (parsedDto.getTypeId() == null) {
            throw this.exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_UPGRADE_TYPE_IS_MANDATORY")
                    .build();
        } else {
            entity.setType(upgradeTypeBo.findByIdOrDie(parsedDto.getTypeId()));
        }
        CrudWithFullRestService.super.beforeSave(parsedDto, entity);
        return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * CrudWithRequirementsRestServiceTrait#getObject()
     */
    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.UPGRADE;
    }

}
