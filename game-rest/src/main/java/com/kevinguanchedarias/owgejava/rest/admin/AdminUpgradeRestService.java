/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.business.UpgradeTypeBo;
import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("admin/upgrade")
public class AdminUpgradeRestService implements CrudWithFullRestService<Integer, Upgrade, UpgradeBo, UpgradeDto>,
		WithImageRestServiceTrait<Integer, Upgrade, UpgradeDto, UpgradeBo> {

	@Autowired
	private UpgradeBo upgradeBo;

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
	public RestCrudConfigBuilder<Integer, Upgrade, UpgradeBo, UpgradeDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, Upgrade, UpgradeBo, UpgradeDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(upgradeBo).withDtoClass(UpgradeDto.class)
				.withEntityClass(Upgrade.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
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
