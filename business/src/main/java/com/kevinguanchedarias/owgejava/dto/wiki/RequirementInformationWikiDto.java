package com.kevinguanchedarias.owgejava.dto.wiki;

import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.RequirementDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;

public class RequirementInformationWikiDto extends RequirementInformationDto {
    @Override
    public RequirementDto getRequirement() {
        return null;
    }

    @Override
    public ObjectRelationDto getRelation() {
        return null;
    }

    public Integer getRequirementId() {
        return super.getRequirement().getId();
    }

    public Integer getRelationId() {
        return super.getRelation().getId();
    }
}
