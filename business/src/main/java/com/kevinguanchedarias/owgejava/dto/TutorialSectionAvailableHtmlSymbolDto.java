package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TutorialSectionAvailableHtmlSymbol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TutorialSectionAvailableHtmlSymbolDto
        implements DtoFromEntity<TutorialSectionAvailableHtmlSymbol> {

    @EqualsAndHashCode.Include
    private Integer id;
    private String name;
    private String identifier;
    private String sectionFrontendPath;

    @Override
    public void dtoFromEntity(TutorialSectionAvailableHtmlSymbol entity) {
        id = entity.getId();
        name = entity.getName();
        identifier = entity.getIdentifier();
        if (entity.getTutorialSection() != null) {
            sectionFrontendPath = entity.getTutorialSection().getFrontendRouterPath();
        }
    }
}
