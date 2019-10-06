package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.math.BigInteger;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.WebsocketMessageStatus;

public interface WebsocketMessageRepository extends JpaRepository<WebsocketMessageStatus, BigInteger>, Serializable {

}
