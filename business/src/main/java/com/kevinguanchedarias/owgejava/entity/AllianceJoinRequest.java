/**
 * 
 */
package com.kevinguanchedarias.owgejava.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

/**
 * Represents a join request for an alliance
 * 
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Entity
@Table(name = "alliance_join_request")
public class AllianceJoinRequest implements SimpleIdEntity {
	private static final long serialVersionUID = -5567072839053714253L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "alliance_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private Alliance alliance;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	@Fetch(FetchMode.JOIN)
	private UserStorage user;

	@Column(name = "request_date", nullable = false)
	private Date requestDate;

	/**
	 * @since 0.7.0
	 * @return the id
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.7.0
	 * @param id
	 *            the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.7.0
	 * @return the alliance
	 */
	public Alliance getAlliance() {
		return alliance;
	}

	/**
	 * @since 0.7.0
	 * @param alliance
	 *            the alliance to set
	 */
	public void setAlliance(Alliance alliance) {
		this.alliance = alliance;
	}

	/**
	 * @since 0.7.0
	 * @return the user
	 */
	public UserStorage getUser() {
		return user;
	}

	/**
	 * @since 0.7.0
	 * @param user
	 *            the user to set
	 */
	public void setUser(UserStorage user) {
		this.user = user;
	}

	/**
	 * @since 0.7.0
	 * @return the requestDate
	 */
	public Date getRequestDate() {
		return requestDate;
	}

	/**
	 * @since 0.7.0
	 * @param requestDate
	 *            the requestDate to set
	 */
	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

}
