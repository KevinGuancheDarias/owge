package com.kevinguanchedarias.owgejava.dto;

import java.math.BigInteger;
import java.util.Date;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.WebsocketMessageStatus;

public class WebsocketMessageStatusDto implements DtoFromEntity<WebsocketMessageStatus> {

	private BigInteger id;
	private String eventName;
	private Boolean unwhilingToDelivery = false;
	private Boolean socketServerAck = false;
	private Boolean socketNotFound = false;
	private Boolean webBrowserAck = false;
	private Boolean isUserAckRequired = false;
	private Date userAck;
	private UserStorageDto user;

	@Override
	public void dtoFromEntity(WebsocketMessageStatus entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		if (entity.getUser() != null) {
			user = new UserStorageDto();
			user.dtoFromEntity(entity.getUser());
		}

	}

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

	public Boolean getSocketServerAck() {
		return socketServerAck;
	}

	public void setSocketServerAck(Boolean socketServerAck) {
		this.socketServerAck = socketServerAck;
	}

	public Boolean getSocketNotFound() {
		return socketNotFound;
	}

	public void setSocketNotFound(Boolean socketNotFound) {
		this.socketNotFound = socketNotFound;
	}

	public Boolean getWebBrowserAck() {
		return webBrowserAck;
	}

	public void setWebBrowserAck(Boolean webBrowserAck) {
		this.webBrowserAck = webBrowserAck;
	}

	public Boolean getIsUserAckRequired() {
		return isUserAckRequired;
	}

	public void setIsUserAckRequired(Boolean isUserAckRequired) {
		this.isUserAckRequired = isUserAckRequired;
	}

	public Date getUserAck() {
		return userAck;
	}

	public void setUserAck(Date userAck) {
		this.userAck = userAck;
	}

	public UserStorageDto getUser() {
		return user;
	}

	public void setUser(UserStorageDto user) {
		this.user = user;
	}

}
