package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@MappedSuperclass
public class CommonEntityWithImageStore<K extends Serializable> extends CommonEntity<K> {
	private static final long serialVersionUID = -5307305337524410867L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "image_id")
	private ImageStore image;

	/**
	 * @since 0.8.0
	 * @return the image
	 */
	public ImageStore getImage() {
		return image;
	}

	/**
	 * @since 0.8.0
	 * @param image the image to set
	 */
	public void setImage(ImageStore image) {
		this.image = image;
	}

}
