package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.WebsocketMessageStatus;

public interface WebsocketMessageRepository extends JpaRepository<WebsocketMessageStatus, Number>, Serializable {

}
