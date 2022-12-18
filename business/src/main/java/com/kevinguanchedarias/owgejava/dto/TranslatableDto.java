package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Translatable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TranslatableDto implements DtoFromEntity<Translatable> {
    @EqualsAndHashCode.Include
    private Long id;

    private String name;
    private String defaultLangCode;
    private TranslatableTranslationDto translation;

    @Override
    public void dtoFromEntity(Translatable entity) {
        id = entity.getId();
        name = entity.getName();
        defaultLangCode = entity.getDefaultLangCode();
        if (entity.getTranslation() != null) {
            translation = new TranslatableTranslationDto();
            translation.dtoFromEntity(entity.getTranslation());
        }
    }

}
