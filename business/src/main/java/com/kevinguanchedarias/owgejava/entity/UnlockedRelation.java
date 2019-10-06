package com.kevinguanchedarias.owgejava.entity;

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

@Entity
@Table(name = "unlocked_relation")
public class UnlockedRelation implements EntityWithId<Long> {
	private static final long serialVersionUID = -5112848052048425078L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "relation_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private ObjectRelation relation;

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public UserStorage getUser() {
		return user;
	}

	public void setUser(UserStorage user) {
		this.user = user;
	}

	public ObjectRelation getRelation() {
		return relation;
	}

	public void setRelation(ObjectRelation relation) {
		this.relation = relation;
	}

}
