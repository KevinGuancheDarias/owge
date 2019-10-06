/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.ImageStore;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
/**
 * @author kevin
 *
 */
public class ImageStoreDto implements WithDtoFromEntityTrait<ImageStore> {
	private Long id;
	private String checksum;
	private String filename;
	private String displayName;
	private String description;
	private String url;

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the checksum
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * @since 0.8.0
	 * @param checksum the checksum to set
	 */
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	/**
	 * @since 0.8.0
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @since 0.8.0
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @since 0.8.0
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @since 0.8.0
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

}
