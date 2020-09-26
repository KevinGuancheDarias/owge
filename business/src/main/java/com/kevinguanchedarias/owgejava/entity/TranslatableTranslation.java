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

import com.kevinguanchedarias.owgejava.entity.listener.TranslatableListener;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Table(name = "translatables_translations")
@Entity
public class TranslatableTranslation implements EntityWithId<Long> {
	private static final long serialVersionUID = 8499028692222579343L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "translatable_id")
	private Translatable translatable;

	@Column(name = "lang_code", length = 2, nullable = false)
	private String langCode;

	private String value;

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
	 * This prop doesn't have a getter, as I don't want to accidentally trigger
	 * lazy-loading, as would enter in a funny infinite loop due to
	 * {@link TranslatableListener#onLoad(Translatable)} loading the
	 * {@link TranslatableTranslation} which triggers a findBy to
	 * {@link Translatable} which... you know.. funny things, who needs games
	 *
	 * @param translatable
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setTranslatable(Translatable translatable) {
		this.translatable = translatable;
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
	 * The value may have frontend-replaced interpolated string for example "Hello
	 * {{userName}}"
	 *
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
