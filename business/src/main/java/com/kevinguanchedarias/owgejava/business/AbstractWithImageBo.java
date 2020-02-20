/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore;
import com.kevinguanchedarias.owgejava.entity.ImageStore;

/**
 * Handles the image saving properly when running a save action to database
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractWithImageBo<K extends Serializable, E extends CommonEntityWithImageStore<K>, D extends DtoFromEntity<E>>
		implements BaseBo<K, E, D> {
	private static final long serialVersionUID = -3656411343369973383L;

	@Autowired
	protected ImageStoreBo imageStoreBo;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#findAll()
	 */
	@Override
	public List<E> findAll() {
		return BaseBo.super.findAll().stream().map(this::handleImage).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#findById(java.io.
	 * Serializable)
	 */
	@Override
	public E findById(K id) {
		return handleImage(BaseBo.super.findById(id));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#findByIdOrDie(java.io.
	 * Serializable)
	 */
	@Override
	public E findByIdOrDie(K id) {
		return handleImage(BaseBo.super.findByIdOrDie(id));
	}

	/**
	 * Saves the entity, it's aware of the image saving, so will make sure the
	 * returned image has its URL
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public E save(E entity) {
		E saved = BaseBo.super.save(entity);
		saved.setImage(imageStoreBo.computeImageUrl(saved.getImage()));
		return saved;
	}

	/**
	 * Handles the images on entity load (populates the transient url property of
	 * {@link ImageStore} <br>
	 * Override to handle entities that has multiple images
	 * 
	 * @param entity
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com
	 */
	public E handleImage(E entity) {
		imageStoreBo.computeImageUrl(entity.getImage());
		return entity;
	}
}
