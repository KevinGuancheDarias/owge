package com.kevinguanchedarias.owgejava.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.entity.TutorialSection;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TutorialSectionDto extends CommonDto<Integer> implements WithDtoFromEntityTrait<TutorialSection> {
	private String frontendRouterPath;
	private List<TutorialSectionAvailableHtmlSymbolDto> availableHtmlSymbols;

	@Override
	public void dtoFromEntity(TutorialSection entity) {
		if (entity.getAvailableHtmlSymbols() != null && !entity.getAvailableHtmlSymbols().isEmpty()) {
			availableHtmlSymbols = entity.getAvailableHtmlSymbols().stream().map(currentSymbol -> {
				TutorialSectionAvailableHtmlSymbolDto dto = new TutorialSectionAvailableHtmlSymbolDto();
				dto.dtoFromEntity(currentSymbol);
				return dto;
			}).collect(Collectors.toList());
		}
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
	}

	/**
	 * @return the frontendRouterPath
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getFrontendRouterPath() {
		return frontendRouterPath;
	}

	/**
	 * @param frontendRouterPath the frontendRouterPath to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setFrontendRouterPath(String frontendRouterPath) {
		this.frontendRouterPath = frontendRouterPath;
	}

	/**
	 * @return the availableHtmlSymbols
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<TutorialSectionAvailableHtmlSymbolDto> getAvailableHtmlSymbols() {
		return availableHtmlSymbols;
	}

	/**
	 * @param availableHtmlSymbols the availableHtmlSymbols to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAvailableHtmlSymbols(List<TutorialSectionAvailableHtmlSymbolDto> availableHtmlSymbols) {
		this.availableHtmlSymbols = availableHtmlSymbols;
	}

}
