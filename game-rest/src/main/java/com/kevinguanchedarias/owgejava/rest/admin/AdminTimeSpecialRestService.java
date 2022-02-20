package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TimeSpecialBo;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@RequestMapping("admin/time_special")
public class AdminTimeSpecialRestService
        implements CrudWithFullRestService<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto>,
        WithImageRestServiceTrait<Integer, TimeSpecial, TimeSpecialDto, TimeSpecialBo> {

    private final TimeSpecialBo timeSpecialBo;
    private final AutowireCapableBeanFactory beanFactory;
    private final DtoUtilService dtoUtilService;
    private final RequirementBo requirementBo;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withBoService(timeSpecialBo).withDtoClass(TimeSpecialDto.class)
                .withEntityClass(TimeSpecial.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * CrudWithRequirementsRestServiceTrait#getObject()
     */
    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.TIME_SPECIAL;
    }

    @Override
    public Optional<TimeSpecial> beforeSave(TimeSpecialDto parsedDto, TimeSpecial entity) {
        CrudWithFullRestService.super.beforeSave(parsedDto, entity);
        return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
    }

    @Override
    public Optional<TimeSpecialDto> beforeRequestEnd(TimeSpecialDto dto, TimeSpecial savedEntity) {
        dto.setRequirements(dtoUtilService.convertEntireArray(RequirementInformationDto.class,
                requirementBo.findRequirements(getObject(), dto.getId())));
        return CrudWithFullRestService.super.beforeRequestEnd(dto, savedEntity);
    }

}
