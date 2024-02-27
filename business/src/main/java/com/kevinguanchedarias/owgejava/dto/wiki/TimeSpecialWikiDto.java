package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.util.ImprovementDtoUtil;

public class TimeSpecialWikiDto extends TimeSpecialDto {
    @Override
    public void dtoFromEntity(TimeSpecial entity) {
        handleCommon(entity);
        handleImageLoad(entity);
        setDuration(entity.getDuration());
        setRechargeTime(entity.getRechargeTime());
        setImprovement(ImprovementDtoUtil.loadImprovementsForWiki(entity));
    }
}
