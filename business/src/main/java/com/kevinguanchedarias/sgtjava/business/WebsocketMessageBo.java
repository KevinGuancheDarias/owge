package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.WebsocketMessageStatus;
import com.kevinguanchedarias.sgtjava.repository.WebsocketMessageRepository;

@Service
public class WebsocketMessageBo implements BaseBo<WebsocketMessageStatus> {
	private static final long serialVersionUID = -6552066142375181519L;

	@Autowired
	private WebsocketMessageRepository websocketMessageRepository;

	@Override
	public JpaRepository<WebsocketMessageStatus, Number> getRepository() {
		return websocketMessageRepository;
	}

}
