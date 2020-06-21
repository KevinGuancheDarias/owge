package com.kevinguanchedarias.owgejava.rest.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithRequirementGroupsRestServiceTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/speed-impact-group")
public class AdminSpeedImpactGroupRestService
		implements CrudRestServiceTrait<Integer, SpeedImpactGroup, SpeedImpactGroupBo, SpeedImpactGroupDto>,
		CrudWithRequirementGroupsRestServiceTrait<SpeedImpactGroup, SpeedImpactGroupBo, SpeedImpactGroupDto> {

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.SPEED_IMPACT_GROUP;
	}

	@Override
	public RestCrudConfigBuilder<Integer, SpeedImpactGroup, SpeedImpactGroupBo, SpeedImpactGroupDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, SpeedImpactGroup, SpeedImpactGroupBo, SpeedImpactGroupDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(speedImpactGroupBo)
				.withDtoClass(SpeedImpactGroupDto.class).withEntityClass(SpeedImpactGroup.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

}
