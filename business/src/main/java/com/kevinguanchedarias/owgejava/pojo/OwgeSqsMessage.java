/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

import java.util.HashMap;
import java.util.Map;

import com.kevinguanchedarias.owgejava.enumerations.OwgeSqsMessageEnum;

/**
 * Represents a message
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class OwgeSqsMessage {
	private OwgeSqsMessageEnum type;
	private Map<String, Object> content;

	/**
	 * @param type
	 * @param content
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public OwgeSqsMessage(OwgeSqsMessageEnum type, Map<String, Object> content) {
		super();
		this.type = type;
		this.content = content;
	}

	/**
	 * @param type
	 * @param content
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public OwgeSqsMessage(OwgeSqsMessageEnum type, Object content) {
		super();
		this.type = type;
		this.content = new HashMap<>();
		this.content.put("content", content);
	}

	/**
	 * As Sometimes we only use the content field, allow to get only it
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Object findSimpleContent() {
		return content.get("content");
	}

	/**
	 * @return the type
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public OwgeSqsMessageEnum getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setType(OwgeSqsMessageEnum type) {
		this.type = type;
	}

	/**
	 * @return the content
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, Object> getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setContent(Map<String, Object> content) {
		this.content = content;
	}

}
