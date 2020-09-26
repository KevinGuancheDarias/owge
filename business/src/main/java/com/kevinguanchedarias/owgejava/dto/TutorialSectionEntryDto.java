package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TutorialSectionEntry;
import com.kevinguanchedarias.owgejava.enumerations.TutorialEventEnum;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TutorialSectionEntryDto implements WithDtoFromEntityTrait<TutorialSectionEntry> {
	private Long id;
	private Integer order;
	private TutorialSectionAvailableHtmlSymbolDto htmlSymbol;
	private TutorialEventEnum event;
	private TranslatableDto text;

	@Override
	public void dtoFromEntity(TutorialSectionEntry entity) {
		if (entity.getHtmlSymbol() != null) {
			htmlSymbol = new TutorialSectionAvailableHtmlSymbolDto();
			htmlSymbol.dtoFromEntity(entity.getHtmlSymbol());
		}
		if (entity.getText() != null) {
			text = new TranslatableDto();
			text.dtoFromEntity(entity.getText());
		}
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
	}

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
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
	public TutorialSectionAvailableHtmlSymbolDto getHtmlSymbol() {
		return htmlSymbol;
	}

	/**
	 * @param htmlSymbol the htmlSymbol to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setHtmlSymbol(TutorialSectionAvailableHtmlSymbolDto htmlSymbol) {
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
	public TranslatableDto getText() {
		return text;
	}

	/**
	 * @param text the text to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setText(TranslatableDto text) {
		this.text = text;
	}

}
