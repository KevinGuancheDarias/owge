package com.kevinguanchedarias.owgejava.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.entity.TranslatableTranslation;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface TranslatableTranslationRepository extends JpaRepository<TranslatableTranslation, Long> {

	/**
	 *
	 * @param translatable
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Optional<TranslatableTranslation> findOneByTranslatableAndLangCode(Translatable translatable,
			String langCode);

	/**
	 *
	 * @param translatableId
	 * @param langCode
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Optional<TranslatableTranslation> findOneByTranslatableIdAndLangCode(Long translatableId, String langCode);

	/**
	 *
	 * @param translatable
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<TranslatableTranslation> findByTranslatable(Translatable translatable);

	/**
	 *
	 * @param id
	 * @param langCode
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TranslatableTranslation findOnesByTranslatableIdAndLangCode(Long id, String langCode);

}
