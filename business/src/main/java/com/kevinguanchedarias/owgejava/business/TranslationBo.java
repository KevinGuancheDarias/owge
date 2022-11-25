package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.TranslatableDto;
import com.kevinguanchedarias.owgejava.dto.TranslatableTranslationDto;
import com.kevinguanchedarias.owgejava.entity.Translatable;
import com.kevinguanchedarias.owgejava.entity.TranslatableTranslation;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.TranslatableRepository;
import com.kevinguanchedarias.owgejava.repository.TranslatableTranslationRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
public class TranslationBo implements BaseBo<Long, Translatable, TranslatableDto> {
    public static final String TRANSLATION_CACHE_TAG = "translation";

    @Serial
    private static final long serialVersionUID = -459016758553450917L;

    @Autowired
    private transient TranslatableRepository repository;

    @Autowired
    private transient TranslatableTranslationRepository translatableTranslationRepository;

    @Autowired
    private transient DtoUtilService dtoUtilService;

    @Override
    public Class<TranslatableDto> getDtoClass() {
        return TranslatableDto.class;
    }

    @Override
    public JpaRepository<Translatable, Long> getRepository() {
        return repository;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<TranslatableTranslationDto> findTranslations(Long translatableId) {
        return translatableTranslationRepository.findByTranslatable(repository.getOne(translatableId)).stream()
                .map(translatable -> dtoUtilService.dtoFromEntity(TranslatableTranslationDto.class, translatable))
                .collect(Collectors.toList());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public TranslatableTranslationDto findTranslationDto(Long translatableId, String langCode) {
        TranslatableTranslation translatableTranslation = findByIdAndLangCode(translatableId, langCode);
        TranslatableTranslationDto retVal = new TranslatableTranslationDto();
        if (translatableTranslation != null) {
            retVal.dtoFromEntity(translatableTranslation);
        }
        return retVal;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public TranslatableTranslation findByIdAndLangCode(Long translatableId, String langCode) {
        Translatable translatable = findByIdOrDie(translatableId);
        Optional<TranslatableTranslation> translation = translatableTranslationRepository
                .findOneByTranslatableAndLangCode(translatable, langCode);
        if (translation.isPresent()) {
            return translation.get();
        } else if (!langCode.equals(translatable.getDefaultLangCode())) {
            translation = translatableTranslationRepository.findOneByTranslatableAndLangCode(translatable,
                    translatable.getDefaultLangCode());
            if (translation.isPresent()) {
                return translation.get();
            }
        }
        return null;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public TranslatableTranslationDto addTranslation(TranslatableDto translatableDto,
                                                     TranslatableTranslationDto translatableTranslationDto) {
        Translatable translatable = repository.findById(translatableDto.getId()).orElse(new Translatable());
        TranslatableTranslation translatableTranslation = translatableTranslationDto.getId() == null
                ? new TranslatableTranslation()
                : translatableTranslationRepository.findById(translatableTranslationDto.getId())
                .orElse(new TranslatableTranslation());
        if (translatable.getId() != null) {
            TranslatableTranslation existingWithLangCode = translatableTranslationRepository
                    .findOnesByTranslatableIdAndLangCode(translatable.getId(),
                            translatableTranslationDto.getLangCode());
            if (existingWithLangCode != null
                    && !existingWithLangCode.getId().equals(translatableTranslationDto.getId())) {
                throw new SgtBackendInvalidInputException("ERR_I18N_LANG_CODE_ALREADY_SPECIFIED");
            }
        }
        translatable.setDefaultLangCode(translatable.getDefaultLangCode());
        translatable.setName(translatableDto.getName());
        translatable = repository.save(translatable);
        translatableTranslation.setTranslatable(translatable);
        translatableTranslation.setLangCode(translatableTranslationDto.getLangCode());
        translatableTranslation.setValue(translatableTranslationDto.getValue());
        return dtoUtilService.dtoFromEntity(TranslatableTranslationDto.class,
                translatableTranslationRepository.save(translatableTranslation));
    }
}
