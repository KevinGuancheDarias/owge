package com.kevinguanchedarias.owgejava.entity;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "system_messages")
public class SystemMessage implements EntityWithId<Integer> {
	private static final long serialVersionUID = 2389321992158592281L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	private String content;

	@Column(name = "creation_date")
	private Date creationDate = new Date();

	@OneToMany(mappedBy = "message", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<UserReadSystemMessage> usersRead;

	/**
	 * @return the id
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	@Override
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
	 * @return the serialversionuid
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * @return the usersRead
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<UserReadSystemMessage> getUsersRead() {
		return usersRead;
	}

	/**
	 * @param usersRead the usersRead to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.16
	 */
	public void setUsersRead(List<UserReadSystemMessage> usersRead) {
		this.usersRead = usersRead;
	}
}
