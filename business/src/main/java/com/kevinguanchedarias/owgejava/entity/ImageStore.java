/**
 * 
 */
package com.kevinguanchedarias.owgejava.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Represents an image stored in the server
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Entity
@Table(name = "images_store")
public class ImageStore implements EntityWithId<Long> {
	private static final long serialVersionUID = 2646635871850664581L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 32, nullable = false)
	private String checksum;

	@Column(length = 500, nullable = false)
	private String filename;

	@Column(name = "display_name", length = 50, nullable = true)
	private String displayName;

	@Column(nullable = true, length = 200)
	private String description = "";

	@Transient
	private String url;

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id the id to set
	 */
	@Override
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * MD5 checksum
	 * 
	 * @since 0.8.0
	 * @return the checksum
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * MD5 checksum
	 * 
	 * @since 0.8.0
	 * @param checksum the checksum to set
	 */
	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	/**
	 * The filename in the server fs
	 * 
	 * @since 0.8.0
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * The filename in the server fs
	 * 
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
	 * 
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
	 * Transient fullpath to url
	 * 
	 * @since 0.8.0
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Transient fullpath to url
	 * 
	 * @since 0.8.0
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

}
