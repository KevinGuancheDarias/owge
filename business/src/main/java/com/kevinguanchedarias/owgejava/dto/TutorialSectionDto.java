package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TutorialSection;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class TutorialSectionDto extends CommonDto<Integer, TutorialSection> {
    private String frontendRouterPath;
    private List<TutorialSectionAvailableHtmlSymbolDto> availableHtmlSymbols;

    @Override
    public void dtoFromEntity(TutorialSection entity) {
        frontendRouterPath = entity.getFrontendRouterPath();
        if (entity.getAvailableHtmlSymbols() != null && !entity.getAvailableHtmlSymbols().isEmpty()) {
            availableHtmlSymbols = entity.getAvailableHtmlSymbols().stream().map(currentSymbol -> {
                TutorialSectionAvailableHtmlSymbolDto dto = new TutorialSectionAvailableHtmlSymbolDto();
                dto.dtoFromEntity(currentSymbol);
                return dto;
            }).collect(Collectors.toList());
        }
    }
}
