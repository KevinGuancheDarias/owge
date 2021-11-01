/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class CommonDtoWithImageStore<K extends Number, E extends CommonEntityWithImageStore<K>> extends CommonDto<K>
        implements WithDtoFromEntityTrait<E> {
    private Long image;
    private String imageUrl;

    @Override
    public void dtoFromEntity(E entity) {
        WithDtoFromEntityTrait.super.dtoFromEntity(entity);
        if (entity.getImage() != null) {
            image = entity.getImage().getId();
            imageUrl = entity.getImage().getUrl();
        }
    }

    /**
     * @since 0.8.0
     * @return the image
     */
    public Long getImage() {
        return image;
    }

    /**
     * @since 0.8.0
     * @param image the image to set
     */
    public void setImage(Long image) {
        this.image = image;
    }

    /**
     * @return the imageUrl
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * @param imageUrl the imageUrl to set
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
