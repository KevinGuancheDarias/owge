/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CommonDtoWithImageStore<K extends Number, E extends CommonEntityWithImageStore<K>> extends CommonDto<K, E> {
    private Long image;
    private String imageUrl;

    @Override
    public void dtoFromEntity(E entity) {
        super.dtoFromEntity(entity);
        if (entity.getImage() != null) {
            image = entity.getImage().getId();
            imageUrl = entity.getImage().getUrl();
        }
    }
}
