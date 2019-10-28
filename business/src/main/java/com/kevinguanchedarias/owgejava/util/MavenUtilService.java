/**
 * 
 */
package com.kevinguanchedarias.owgejava.util;

import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Has methods for interacting with the maven available properties
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class MavenUtilService {
	private static final Logger LOG = Logger.getLogger(MavenUtilService.class);

	private String version;

	/**
	 * Find the project version in time
	 * 
	 * @see <a href="https://stackoverflow.com/a/12571330/1922558">Related
	 *      StackOverflow answer</a>
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostConstruct
	public void init() {
		try {
			Properties p = new Properties();
			InputStream is = getClass().getResourceAsStream(
					"/META-INF/maven/com.kevinguanchedarias.owgejava/owgejava-backend/pom.properties");
			if (is != null) {
				p.load(is);
				version = p.getProperty("version", "");
			}
		} catch (Exception e) {
			// ignore
		}

		// fallback to using Java API
		if (version == null) {
			Package aPackage = getClass().getPackage();
			if (aPackage != null) {
				version = aPackage.getImplementationVersion();
				if (version == null) {
					version = aPackage.getSpecificationVersion();
				}
			}
		}
		version = normalizeVersion(version);
		LOG.debug("Project maven version is: " + version);
	}

	/**
	 * Finds the project maven version, if not possible will fallback to "master"
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String findVersion() {
		return findVersion("master");
	}

	/**
	 * Finds the project maven version (if not possible will return the
	 * <i>fallbackValue</i>)
	 * 
	 * @param fallbackValue Value to use when not able to resolve the version
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String findVersion(String fallbackValue) {
		if (StringUtils.isEmpty(version)) {
			LOG.info("Not able to resolve project version, defaulting to: " + fallbackValue);
			return fallbackValue;
		} else {
			return version;
		}
	}

	private String normalizeVersion(String version) {
		return version == null ? version : version.replaceAll("-SNAPSHOT", "");

	}
}
