package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class TranslatableDto implements WithDtoFromEntityTrait<Translatable> {
	private Long id;
	private String name;
	private String defaultLangCode;
	private TranslatableTranslationDto translation;

	@Override
	public void dtoFromEntity(Translatable entity) {
		if (entity.getTranslation() != null) {
			translation = new TranslatableTranslationDto();
			translation.dtoFromEntity(entity.getTranslation());
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
	 * @return the defaultLangCode
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getDefaultLangCode() {
		return defaultLangCode;
	}

	/**
	 * @param defaultLangCode the defaultLangCode to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setDefaultLangCode(String defaultLangCode) {
		this.defaultLangCode = defaultLangCode;
	}

	/**
	 * @return the translation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TranslatableTranslationDto getTranslation() {
		return translation;
	}

	/**
	 * @param translation the translation to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setTranslation(TranslatableTranslationDto translation) {
		this.translation = translation;
	}

}
