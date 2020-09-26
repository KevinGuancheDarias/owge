package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Represents a tutorial Section
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "tutorial_sections")
public class TutorialSection extends CommonEntity<Integer> {
	private static final long serialVersionUID = -8797092312548538813L;

	@Column(name = "frontend_router_path", length = 150)
	private String frontendRouterPath;

	@OneToMany(mappedBy = "tutorialSection")
	private transient List<TutorialSectionAvailableHtmlSymbol> availableHtmlSymbols;

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
	public List<TutorialSectionAvailableHtmlSymbol> getAvailableHtmlSymbols() {
		return availableHtmlSymbols;
	}

	/**
	 * @param availableHtmlSymbols the availableHtmlSymbols to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAvailableHtmlSymbols(List<TutorialSectionAvailableHtmlSymbol> availableHtmlSymbols) {
		this.availableHtmlSymbols = availableHtmlSymbols;
	}
}
