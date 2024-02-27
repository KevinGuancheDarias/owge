package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.UpgradeDto;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.util.ImprovementDtoUtil;

public class UpgradeWikiDto extends UpgradeDto {
    @Override
    public void dtoFromEntity(Upgrade entity) {
        handleCommon(entity);
        handleImageLoad(entity);
        loadData(entity);
        var type = entity.getType();
        setTypeId(type != null ? type.getId() : null);
        setImprovement(ImprovementDtoUtil.loadImprovementsForWiki(entity));
    }
}
