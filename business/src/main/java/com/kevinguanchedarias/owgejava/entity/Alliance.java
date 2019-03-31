package com.kevinguanchedarias.owgejava.entity;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * Represents an alliance of players
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Entity
@Table(name = "alliances")
public class Alliance extends CommonEntityWithImage<Integer> {
	private static final long serialVersionUID = -3191006475065220996L;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private UserStorage owner;

	@OneToMany(mappedBy = "alliance")
	private List<UserStorage> users;

	/**
	 * @since 0.7.0
	 * @return the owner
	 */
	public UserStorage getOwner() {
		return owner;
	}

	/**
	 * @since 0.7.0
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(UserStorage owner) {
		this.owner = owner;
	}

	/**
	 * @since 0.7.0
	 * @return the users
	 */
	public List<UserStorage> getUsers() {
		return users;
	}

	/**
	 * @since 0.7.0
	 * @param users
	 *            the users to set
	 */
	public void setUsers(List<UserStorage> users) {
		this.users = users;
	}

}
