/**
 * 
 */
package com.kevinguanchedarias.owgejava.enumerations;

import org.springframework.util.StringUtils;

/**
 * Has the possible values for document type inside the Git docs
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public enum DocTypeEnum {
	EXCEPTIONS;

	private String customPath;

	private DocTypeEnum() {

	}

	/**
	 * Finds the path for the given enum type
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String findPath() {
		return StringUtils.isEmpty(customPath) ? name().toLowerCase() : customPath;
	}
}
