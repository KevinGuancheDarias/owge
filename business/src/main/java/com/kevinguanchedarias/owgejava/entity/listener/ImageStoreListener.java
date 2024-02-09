package com.kevinguanchedarias.owgejava.entity.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.entity.ImageStore;

/**
 * Fills the transient properties of ImageStore
 *
 * @author Kevin Guanche Darias
 * @since 0.9.0
 */
@Component
@Lazy
public class ImageStoreListener {
    private ImageStoreBo imageStoreBo;

    public ImageStoreListener(ImageStoreBo imageStoreBo) {
        this.imageStoreBo = imageStoreBo;
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void postLoad(ImageStore imageStore) {
        imageStoreBo.computeImageUrl(imageStore);

    }
}
