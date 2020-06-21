package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;

/**
 * Due to entity name, in order to avoid confusions and having to manually put
 * java.lang.Object, this class name is ObjectEntity
 *
 * @author Kevin Guanche Darias
 */
@Entity
@Table(name = "objects")
public class ObjectEntity implements Serializable {
	private static final long serialVersionUID = 418080945672588722L;

	@Id
	private String description;

	@Transient
	private String code;

	private String repository;

	/**
	 *
	 * @deprecated To avoid confusions use {@link ObjectEntity#getCode()}
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public String getDescription() {
		return description;
	}

	/**
	 *
	 * @deprecated To avoid confusions use {@link ObjectEntity#setCode(String)}
	 * @param description
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @todo In the future remove <i>description</i>, and set code as the id, and
	 *       return it instead of returning <i>description</i>
	 * @since 0.8.0
	 * @return the code
	 */
	public String getCode() {
		return description;
	}

	/**
	 * @since 0.8.0
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.description = code;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String entity) {
		this.repository = entity;
	}

	/**
	 * Finds the code as enum, (to avoid having to clone that too many times)
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectEnum findCodeAsEnum() {
		return ObjectEnum.valueOf(getCode());
	}
}