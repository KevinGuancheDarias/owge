package com.kevinguanchedarias.owgejava.entity.listener;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.entity.ImageStore;

/**
 * Fills the transient properties of ImageStore
 * 
 * @author Kevin Guanche Darias
 * @since 0.9.0
 *
 */
public class ImageStoreListener {
	private ImageStoreBo imageStoreBo;

	@PostLoad
	@PostPersist
	@PostUpdate
	public void postLoad(ImageStore imageStore) {
		if (!Thread.currentThread().getName().startsWith("OWGE_BACKGROUND_")) {
			findImageStoreBo().computeImageUrl(imageStore);
		}
	}

	private ImageStoreBo findImageStoreBo() {
		if (imageStoreBo == null) {
			WebApplicationContext cc = ContextLoader.getCurrentWebApplicationContext();
			imageStoreBo = cc.getAutowireCapableBeanFactory().getBean(ImageStoreBo.class);
		}
		return imageStoreBo;
	}
}
