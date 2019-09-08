/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UserPojo {
	private Integer id;
	private String username;
	private String email;

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id
	 *            the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @since 0.8.0
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @since 0.8.0
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @since 0.8.0
	 * @param email
	 *            the email to set
	 */
	public void setEmail(String email) {
		this.email = email;
	}

}
