package com.kevinguanchedarias.sgtjava.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "universes")
public class Universe implements Serializable {
	private static final long serialVersionUID = -8162253946714731275L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 50, nullable = false, unique = true)
	private String name;

	@Column(length = 500)
	private String description;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "creator", nullable = true)
	@JsonIgnore
	private User creator;

	@Column(name = "creation_date", nullable = false)
	private Date creationDate = new Date();

	@Column(name = "public", nullable = false)
	private Boolean isPublic = false;

	@Column(name = "official", nullable = false)
	private Boolean official = false;

	@Column(name = "target_database", length = 50, nullable = false)
	@JsonIgnore
	private String targetDatabase;

	@Column(name = "rest_base_url", length = 200, nullable = false)
	private String restBaseUrl;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date date) {
		this.creationDate = date;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Boolean getOfficial() {
		return official;
	}

	public void setOfficial(Boolean official) {
		this.official = official;
	}

	public String getTargetDatabase() {
		return targetDatabase;
	}

	public void setTargetDatabase(String targetDatabase) {
		this.targetDatabase = targetDatabase;
	}

	public String getRestBaseUrl() {
		return restBaseUrl;
	}

	public void setRestBaseUrl(String restBaseUrl) {
		this.restBaseUrl = restBaseUrl;
	}
}
