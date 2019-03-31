package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * 
 * @deprecated As of <b>0.7.0</b> is better to use {@link CommonEntityWithImage}
 * @since 0.1.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated
@MappedSuperclass
public abstract class EntityWithImage {
	private String image;

	@Column(name = "image")
	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

}
