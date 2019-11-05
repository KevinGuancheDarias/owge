package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * 
 * @deprecated No longer store the current values in the database
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated(since = "0.8.0")
@Entity
@Table(name = "user_improvements")
public class UserImprovement extends ImprovementBase<Integer> {
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
