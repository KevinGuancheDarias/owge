package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.entity.ImageStore;

/**
 * Fills the transient properties of ImageStore
 * 
 * @author Kevin Guanche Darias
 * @since 0.9.0
 *
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
		if (!Thread.currentThread().getName().startsWith("OWGE_BACKGROUND_")) {
			imageStoreBo.computeImageUrl(imageStore);
		}
	}
}
