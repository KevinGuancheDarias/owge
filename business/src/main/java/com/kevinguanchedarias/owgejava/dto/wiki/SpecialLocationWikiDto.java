package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.SpecialLocationDto;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.util.ImprovementDtoUtil;

public class SpecialLocationWikiDto extends SpecialLocationDto {
    @Override
    public void dtoFromEntity(SpecialLocation entity) {
        handleCommon(entity);
        handleImageLoad(entity);
        loadGalaxy(entity);
        setImprovement(ImprovementDtoUtil.loadImprovementsForWiki(entity));
    }
}
