package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.kevinguanchedarias.owgejava.exception.InvalidInputException;

@Entity
@Table(name = "users")
public class User implements Serializable {
	private static final long serialVersionUID = 1631617806515618993L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 32, unique = true, nullable = false)
	private String username;

	@Column(length = 254, unique = true, nullable = false)
	private String email;

	@Column(length = 190, nullable = false)
	private String password;

	private Boolean activated = false;

	@Column(name = "creation_date", nullable = false)
	private Date creationDate = new Date();

	@Column(name = "last_login", nullable = false)
	private Date lastLogin = new Date();

	@Column(name = "first_name", length = 50)
	private String firstName;

	@Column(name = "last_name", length = 50)
	private String lastName;

	private Boolean notifications = false;

	/**
	 * Will check that user is valid
	 * 
	 * @throws InvalidInputException
	 * @author Kevin Guanche Darias
	 */
	public void checkValid() {
		if ("system".equals(username)) {
			throw new InvalidInputException("Username cannot be \"system\", reserved keyword");
		}
		throwRequired(username, "username");
		throwRequired(email, "email");
		throwRequired(password, "password");
		throwRequired(creationDate, "creationDate");
		throwRequired(lastLogin, "lastLogin");
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Boolean getActivated() {
		return activated;
	}

	public void setActivated(Boolean activated) {
		this.activated = activated;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(Date lastLogin) {
		this.lastLogin = lastLogin;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Boolean getNotifications() {
		return notifications;
	}

	public void setNotifications(Boolean notifications) {
		this.notifications = notifications;
	}

	/**
	 * Will throw exception when field it's missing
	 * 
	 * @param field
	 *            Object to null-check
	 * @param fieldName
	 * @author Kevin Guanche Darias
	 */
	private void throwRequired(Object field, String fieldName) {
		if (field == null) {
			throw new InvalidInputException("Missing field " + fieldName);
		}
	}
}
