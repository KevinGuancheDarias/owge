package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TimeSpecialDto extends CommonDtoWithImageStore<Integer, TimeSpecial> implements DtoWithImprovements {

    private Long duration;
    private Long rechargeTime;
    private ImprovementDto improvement;
    private ActiveTimeSpecialDto activeTimeSpecialDto;
    private List<RequirementInformationDto> requirements;

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.owgejava.dto.DtoFromEntity#dtoFromEntity(java.lang
     * .Object)
     */
    @Override
    public void dtoFromEntity(TimeSpecial entity) {
        duration = entity.getDuration();
        rechargeTime = entity.getRechargeTime();
        DtoWithImprovements.super.dtoFromEntity(entity);
        super.dtoFromEntity(entity);
    }
}
