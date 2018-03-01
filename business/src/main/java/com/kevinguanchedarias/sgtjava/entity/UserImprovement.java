package com.kevinguanchedarias.sgtjava.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_improvements")
public class UserImprovement extends ImprovementBase {
	private static final long serialVersionUID = -2450393006005853525L;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserStorage user;

	public UserStorage getUser() {
		return user;
	}

	public void setUser(UserStorage user) {
		this.user = user;
	}

}
