package com.kevinguanchedarias.owgejava.entity;

import lombok.EqualsAndHashCode;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Represents a CommonEntity that also has an image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @deprecated Image should be managed by {@link ImageStore} , so use
 * {@link CommonEntityWithImageStore}
 */
@Deprecated(since = "0.8.0")
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public class CommonEntityWithImage<K extends Number> extends CommonEntity<K> {
    private static final long serialVersionUID = -3650030382618207233L;

    @EqualsAndHashCode.Exclude
    private String image;

    @Column(name = "image")
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
