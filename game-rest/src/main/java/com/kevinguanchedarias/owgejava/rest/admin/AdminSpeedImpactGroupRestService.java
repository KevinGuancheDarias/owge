package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.SpeedImpactGroupDto;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithRequirementGroupsRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/speed-impact-group")
@AllArgsConstructor
public class AdminSpeedImpactGroupRestService
        implements CrudRestServiceTrait<Integer, SpeedImpactGroup, SpeedImpactGroupRepository, SpeedImpactGroupDto>,
        CrudWithRequirementGroupsRestServiceTrait<SpeedImpactGroup, SpeedImpactGroupRepository, SpeedImpactGroupDto> {

    private final AutowireCapableBeanFactory beanFactory;
    private final SpeedImpactGroupRepository SpeedImpactGroupRepository;

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.SPEED_IMPACT_GROUP;
    }

    @Override
    public RestCrudConfigBuilder<Integer, SpeedImpactGroup, SpeedImpactGroupRepository, SpeedImpactGroupDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, SpeedImpactGroup, SpeedImpactGroupRepository, SpeedImpactGroupDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withRepository(SpeedImpactGroupRepository)
                .withDtoClass(SpeedImpactGroupDto.class).withEntityClass(SpeedImpactGroup.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    @Override
    public Optional<SpeedImpactGroup> beforeSave(SpeedImpactGroupDto parsedDto, SpeedImpactGroup entity) {
        if (parsedDto.getImage() == null) {
            entity.setImage(null);
        } else {
            entity.setImage(getRestCrudConfigBuilder().build().getBeanFactory().getBean(ImageStoreBo.class)
                    .findByIdOrDie(parsedDto.getImage()));
        }
        return CrudRestServiceTrait.super.beforeSave(parsedDto, entity);
    }
}
