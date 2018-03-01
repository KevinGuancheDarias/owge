package com.kevinguanchedarias.sgtjava.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "configuration")
public class Configuration implements Serializable {
	private static final long serialVersionUID = -298326125776225265L;

	@Id
	private String name;

	@Column(name = "display_name", length = 400, nullable = true)
	private String displayName;

	private String value;

	private Boolean privileged = false;

	public Configuration() {
		super();
	}

	public Configuration(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public Configuration(String name, String value, String displayName) {
		this.name = name;
		this.value = value;
		this.displayName = displayName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Boolean getPrivileged() {
		return privileged;
	}

	public void setPrivileged(Boolean privileged) {
		this.privileged = privileged;
	}
}
