package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TutorialSectionEntry;
import com.kevinguanchedarias.owgejava.enumerations.TutorialEventEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TutorialSectionEntryDto implements DtoFromEntity<TutorialSectionEntry> {

    @EqualsAndHashCode.Include
    private Long id;

    private Integer order;
    private TutorialEventEnum event;
    private TutorialSectionAvailableHtmlSymbolDto htmlSymbol;
    private TranslatableDto text;

    @Override
    public void dtoFromEntity(TutorialSectionEntry entity) {
        id = entity.getId();
        order = entity.getOrder();
        event = entity.getEvent();
        if (entity.getHtmlSymbol() != null) {
            htmlSymbol = new TutorialSectionAvailableHtmlSymbolDto();
            htmlSymbol.dtoFromEntity(entity.getHtmlSymbol());
        }
        if (entity.getText() != null) {
            text = new TranslatableDto();
            text.dtoFromEntity(entity.getText());
        }
    }
}
