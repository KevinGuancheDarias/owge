package com.kevinguanchedarias.owgejava.pojo;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.kevinguanchedarias.owgejava.entity.WebsocketMessageStatus;

/**
 * This class is used to keep trace of the delivery status of the websocket
 * messages
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class DeliveryQueueEntry {
	private CompletableFuture<WebsocketMessageStatus> relatedPromise;
	private Map<String, Object> data;
	private Map<String, Object> clientData;

	public DeliveryQueueEntry(CompletableFuture<WebsocketMessageStatus> relatedPromise, Map<String, Object> data) {
		this.relatedPromise = relatedPromise;
		this.data = data;
	}

	public CompletableFuture<WebsocketMessageStatus> getRelatedPromise() {
		return relatedPromise;
	}

	public void setRelatedPromise(CompletableFuture<WebsocketMessageStatus> relatedPromise) {
		this.relatedPromise = relatedPromise;
	}

	/**
	 * Returns the backend passed data to this socket message
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	/**
	 * Returns the client data (Usually browser response)
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, Object> getClientData() {
		return clientData;
	}

	public void setClientData(Map<String, Object> clientData) {
		this.clientData = clientData;
	}

}
