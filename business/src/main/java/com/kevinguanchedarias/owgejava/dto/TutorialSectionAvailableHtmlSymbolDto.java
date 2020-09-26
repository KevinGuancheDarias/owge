package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TutorialSectionAvailableHtmlSymbol;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TutorialSectionAvailableHtmlSymbolDto
		implements WithDtoFromEntityTrait<TutorialSectionAvailableHtmlSymbol> {

	private Integer id;
	private String name;
	private String identifier;
	private String sectionFrontendPath;

	@Override
	public void dtoFromEntity(TutorialSectionAvailableHtmlSymbol entity) {
		if (entity.getTutorialSection() != null) {
			sectionFrontendPath = entity.getTutorialSection().getFrontendRouterPath();
		}
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
	}

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the identifier
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier the identifier to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * @return the sectionFrontendPath
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getSectionFrontendPath() {
		return sectionFrontendPath;
	}

	/**
	 * @param sectionFrontendPath the sectionFrontendPath to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSectionFrontendPath(String sectionFrontendPath) {
		this.sectionFrontendPath = sectionFrontendPath;
	}

}
