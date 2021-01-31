package com.kevinguanchedarias.owgejava.pojo;

import java.util.Date;

/**
 * Represents a system message with the is read property for the given user
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
/**
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class SystemMessageUser {
	private Integer id;
	private String content;
	private Date creationDate;
	private boolean isRead;

	/**
	 * @return the id
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the content
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the creationDate
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * @param creationDate the creationDate to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return the isRead
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean getIsRead() {
		return isRead;
	}

	/**
	 * @param isRead the isRead to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setIsRead(boolean isRead) {
		this.isRead = isRead;
	}

}
