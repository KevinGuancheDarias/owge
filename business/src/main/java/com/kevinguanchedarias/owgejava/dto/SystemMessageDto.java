package com.kevinguanchedarias.owgejava.dto;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class SystemMessageDto implements WithDtoFromEntityTrait<SystemMessage> {
	private Integer id;

	private String content;
	private Date creationDate = new Date();

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

}
