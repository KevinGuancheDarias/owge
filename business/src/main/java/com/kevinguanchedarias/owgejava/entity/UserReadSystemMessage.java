package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents that the user has read the message
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "user_read_system_messages")
public class UserReadSystemMessage implements EntityWithId<Long> {
	private static final long serialVersionUID = 6740014767892661497L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserStorage user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "message_id", nullable = false)
	private SystemMessage message;

	/**
	 * @return the id
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the user
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setUser(UserStorage user) {
		this.user = user;
	}

	/**
	 * @return the message
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SystemMessage getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setMessage(SystemMessage message) {
		this.message = message;
	}

}
