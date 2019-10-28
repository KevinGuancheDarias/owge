/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

/**
 * Represents the JSON required to upload a image
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UploadImage {
	private String displayName;
	private String base64;

	/**
	 * @since 0.8.0
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @since 0.8.0
	 * @param displayName the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @since 0.8.0
	 * @return the base64
	 */
	public String getBase64() {
		return base64;
	}

	/**
	 * @since 0.8.0
	 * @param base64 the base64 to set
	 */
	public void setBase64(String base64) {
		this.base64 = base64;
	}

}
