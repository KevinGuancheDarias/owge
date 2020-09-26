package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.kevinguanchedarias.owgejava.entity.listener.TranslatableListener;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@EntityListeners(TranslatableListener.class)
@Table(name = "translatables")
public class Translatable implements EntityWithId<Long> {
	private static final long serialVersionUID = -5334666049127689262L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 100, nullable = false)
	private String name;

	@Column(name = "default_lang_code", length = 2, nullable = false)
	private String defaultLangCode = "en";

	@Transient
	private TranslatableTranslation translation;

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
	public TranslatableTranslation getTranslation() {
		return translation;
	}

	/**
	 * @param translation the translation to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setTranslation(TranslatableTranslation translation) {
		this.translation = translation;
	}

}
