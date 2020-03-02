package com.kevinguanchedarias.owgejava.rest.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;

@RestController
@ApplicationScope
@RequestMapping("admin/unit")
public class AdminUnitRestService implements CrudWithFullRestService<Integer, Unit, UnitBo, UnitDto>,
		WithImageRestServiceTrait<Integer, Unit, UnitDto, UnitBo> {

	@Autowired
	private UnitBo unitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

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
	public RestCrudConfigBuilder<Integer, Unit, UnitBo, UnitDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, Unit, UnitBo, UnitDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(unitBo).withDtoClass(UnitDto.class)
				.withEntityClass(Unit.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	@Override
	public Optional<Unit> beforeSave(UnitDto parsedDto, Unit entity) {
		if (parsedDto.getTypeId() == null) {
			throw this.exceptionUtilService
					.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_UPGRADE_TYPE_IS_MANDATORY")
					.build();
		} else {
			entity.setType(unitTypeBo.findByIdOrDie(parsedDto.getTypeId()));
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
