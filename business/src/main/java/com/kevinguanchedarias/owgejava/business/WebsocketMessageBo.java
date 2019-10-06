package com.kevinguanchedarias.owgejava.business;

import java.math.BigInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.WebsocketMessageStatus;
import com.kevinguanchedarias.owgejava.repository.WebsocketMessageRepository;

@Service
public class WebsocketMessageBo
		implements BaseBo<BigInteger, WebsocketMessageStatus, DtoFromEntity<WebsocketMessageStatus>> {
	private static final long serialVersionUID = -6552066142375181519L;

	@Autowired
	private WebsocketMessageRepository websocketMessageRepository;

	@Override
	public JpaRepository<WebsocketMessageStatus, BigInteger> getRepository() {
		return websocketMessageRepository;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
	 */
	@Override
	public Class<DtoFromEntity<WebsocketMessageStatus>> getDtoClass() {
		return null;
	}

}
