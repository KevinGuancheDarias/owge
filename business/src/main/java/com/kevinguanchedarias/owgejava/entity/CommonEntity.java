package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * Represents an entity with the common parameters:
 * 
 * <ul>
 * <li>id</li>
 * <li>name</li>
 * <li>description</li>
 * </ul>
 * 
 *
 * @param <K>
 *            Id type
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@MappedSuperclass
public abstract class CommonEntity<K extends Number> implements SimpleIdEntity {
	private static final long serialVersionUID = -5044252651188741213L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private K id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", nullable = true)
	private String description;

	@Override
	public K getId() {
		return id;
	}

	public void setId(K id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
