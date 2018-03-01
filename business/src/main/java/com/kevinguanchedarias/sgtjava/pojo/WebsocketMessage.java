package com.kevinguanchedarias.sgtjava.pojo;

import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;

public class WebsocketMessage {

	private Object value;

	private String protocol;

	private String status;

	public WebsocketMessage(String value, String protocol) {
		this.value = value;
		this.protocol = protocol;
		this.status = "ok";
	}

	public WebsocketMessage(Object value, String protocol, String status) {
		this.value = value;
		this.protocol = protocol;
		setStatus(status);
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if (status != "ok" && status != "error") {
			throw new ProgrammingException("invalid value supplied to status");
		}
		this.status = status;
	}

}
