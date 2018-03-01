package com.kevinguanchedarias.sgtjava.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
	
	private String repository;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String entity) {
		this.repository = entity;
	}

}