package com.kevinguanchedarias.owgejava.pojo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Adds deprecation fields to a rest JSON response
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class DeprecationRestResponse<T> {
	private Map<String, String> deprecated = new HashMap<>();

	@JsonUnwrapped
	private T originalData;

	/**
	 * As we can't Unwrap a child JSON object inside an array for obvious reasons,
	 * we have to add the deprecation warning to all child objects
	 *
	 * @param <T>
	 * @param sinceVersion
	 * @param useInstead
	 * @param inputDeprecation
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <T> List<DeprecationRestResponse<T>> fromList(String sinceVersion, String useInstead,
			List<T> inputDeprecation) {
		return inputDeprecation.stream()
				.map(current -> new DeprecationRestResponse<>(sinceVersion, useInstead, current))
				.collect(Collectors.toList());
	}

	/**
	 *
	 * @param sinceVersion
	 * @param useInstead
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public DeprecationRestResponse(String sinceVersion, String useInstead, T originalData) {
		deprecated.put("sinceVersion", sinceVersion);
		deprecated.put("useInstead", useInstead);
		this.originalData = originalData;
	}

	/**
	 * @return the deprecated
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, String> getDeprecated() {
		return deprecated;
	}

	/**
	 * @param deprecated the deprecated to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setDeprecated(Map<String, String> deprecated) {
		this.deprecated = deprecated;
	}

	/**
	 * @return the originalData
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public T getOriginalData() {
		return originalData;
	}

	/**
	 * @param originalData the originalData to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setOriginalData(T originalData) {
		this.originalData = originalData;
	}

}
