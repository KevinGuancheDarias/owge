package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TranslatableTranslation;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TranslatableTranslationDto implements WithDtoFromEntityTrait<TranslatableTranslation> {

	private Long id;
	private String langCode;
	private String value;

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
	 * @return the langCode
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getLangCode() {
		return langCode;
	}

	/**
	 * @param langCode the langCode to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	/**
	 * @return the value
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
