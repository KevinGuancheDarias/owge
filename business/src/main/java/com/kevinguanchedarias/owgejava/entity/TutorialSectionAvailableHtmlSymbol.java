package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Represents a referenced html element that is available in a
 * {@link TutorialSection}
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "tutorial_sections_available_html_symbols")
public class TutorialSectionAvailableHtmlSymbol implements EntityWithId<Integer> {
	private static final long serialVersionUID = 5287466924892458677L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50)
	private String name;

	@Column(length = 150)
	private String identifier;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "tutorial_section_id")
	@Fetch(FetchMode.JOIN)
	private TutorialSection tutorialSection;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
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
	 * When the section is null, it means the element is globally available
	 *
	 * @return the tutorialSection
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TutorialSection getTutorialSection() {
		return tutorialSection;
	}

	/**
	 * @param tutorialSection the tutorialSection to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setTutorialSection(TutorialSection tutorialSection) {
		this.tutorialSection = tutorialSection;
	}

}
