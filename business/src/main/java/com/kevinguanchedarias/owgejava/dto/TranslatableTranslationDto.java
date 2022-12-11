package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.TranslatableTranslation;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TranslatableTranslationDto implements DtoFromEntity<TranslatableTranslation> {
    private Long id;
    private String langCode;
    private String value;

    @Override
    public void dtoFromEntity(TranslatableTranslation entity) {
        id = entity.getId();
        langCode = entity.getLangCode();
        value = entity.getValue();
    }
}
