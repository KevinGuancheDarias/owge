package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "obtained_upgrades")
public class ObtainedUpgrade implements SimpleIdEntity {
	private static final long serialVersionUID = 2859853666452009827L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private UserStorage userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "upgrade_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private Upgrade upgrade;

	@Column(nullable = false)
	private Integer level;

	@Column(nullable = false)
	private Boolean available;

	@Override
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserStorage getUserId() {
		return userId;
	}

	public void setUserId(UserStorage userId) {
		this.userId = userId;
	}

	public Upgrade getUpgrade() {
		return upgrade;
	}

	public void setUpgrade(Upgrade upgrade) {
		this.upgrade = upgrade;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public Boolean getAvailable() {
		return available;
	}

	public void setAvailable(Boolean available) {
		this.available = available;
	}

}