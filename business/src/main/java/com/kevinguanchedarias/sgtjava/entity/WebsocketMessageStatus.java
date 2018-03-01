package com.kevinguanchedarias.sgtjava.entity;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;

@Entity
@Table(name = "websocket_messages_status")
public class WebsocketMessageStatus implements SimpleIdEntity {
	private static final long serialVersionUID = 5440664489889906269L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private BigInteger id;

	@Column(name = "event_name", length = 100, nullable = false)
	private String eventName;

	@Column(name = "unwhiling_to_delivery", nullable = false)
	private Boolean unwhilingToDelivery = false;

	@Column(name = "socket_server_ack", nullable = false)
	private Boolean socketServerAck = false;

	@Column(name = "socket_not_found", nullable = false)
	private Boolean socketNotFound = false;

	@Column(name = "web_browser_ack", nullable = false)
	private Boolean webBrowserAck = false;

	@Column(name = "is_user_ack_required", nullable = false)
	private Boolean isUserAckRequired = false;

	@Column(name = "user_ack", nullable = true)
	private Date userAck;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "user_id", nullable = true)
	@Fetch(FetchMode.JOIN)
	private UserStorage user;

	@Override
	public BigInteger getId() {
		return id;
	}

	public void setId(BigInteger id) {
		this.id = id;
	}

	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public Boolean getUnwhilingToDelivery() {
		return unwhilingToDelivery;
	}

	public void setUnwhilingToDelivery(Boolean unwhilingToDelivery) {
		this.unwhilingToDelivery = unwhilingToDelivery;
	}

	/**
	 * Socket server received the ACK
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getSocketServerAck() {
		return socketServerAck;
	}

	public void setSocketServerAck(Boolean socketServerAck) {
		this.socketServerAck = socketServerAck;
	}

	/**
	 * Socket server notified, that the client is not connected to a websocket
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getSocketNotFound() {
		return socketNotFound;
	}

	public void setSocketNotFound(Boolean socketNotFound) {
		this.socketNotFound = socketNotFound;
	}

	/**
	 * The web browser of the client received the message
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getWebBrowserAck() {
		return webBrowserAck;
	}

	public void setWebBrowserAck(Boolean webBrowserAck) {
		this.webBrowserAck = webBrowserAck;
	}

	/**
	 * True if it's required to have userAck defined <br>
	 * If required, should <b>not</b> consider message as delivered unless the
	 * user ack is defined
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getIsUserAckRequired() {
		return isUserAckRequired;
	}

	public void setIsUserAckRequired(Boolean isUserAckRequired) {
		this.isUserAckRequired = isUserAckRequired;
	}

	/**
	 * When did the user read the message, if done
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getUserAck() {
		return userAck;
	}

	public void setUserAck(Date userAck) {
		this.userAck = userAck;
	}

	/**
	 * Gets the target user, this user can be null, if target is somehow system
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserStorage getUser() {
		return user;
	}

	public void setUser(UserStorage user) {
		this.user = user;
	}

}
