package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.owgejava.enumerations.TutorialEventEnum;

/**
 * Represents an entry of tutorial
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "tutorial_sections_entries")
public class TutorialSectionEntry implements EntityWithId<Long> {
	private static final long serialVersionUID = -2220422909343540317L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_num")
	private Integer order;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "section_available_html_symbol_id")
	@Fetch(FetchMode.JOIN)
	private TutorialSectionAvailableHtmlSymbol htmlSymbol;

	@Enumerated(EnumType.STRING)
	private TutorialEventEnum event;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "text_id")
	@Fetch(FetchMode.JOIN)
	private Translatable text;

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the order
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getOrder() {
		return order;
	}

	/**
	 * @param order the order to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * @return the htmlSymbol
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TutorialSectionAvailableHtmlSymbol getHtmlSymbol() {
		return htmlSymbol;
	}

	/**
	 * @param htmlSymbol the htmlSymbol to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setHtmlSymbol(TutorialSectionAvailableHtmlSymbol htmlSymbol) {
		this.htmlSymbol = htmlSymbol;
	}

	/**
	 * @return the event
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TutorialEventEnum getEvent() {
		return event;
	}

	/**
	 * @param event the event to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEvent(TutorialEventEnum event) {
		this.event = event;
	}

	/**
	 * @return the text
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Translatable getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setText(Translatable text) {
		this.text = text;
	}

}
