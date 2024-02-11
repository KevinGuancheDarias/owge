/**
 *
 */
package com.kevinguanchedarias.owgejava.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Has methods for interacting with the maven available properties
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
@Slf4j
public class MavenUtilService {

    private String version;

    /**
     * Find the project version in time
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @see <a href="https://stackoverflow.com/a/12571330/1922558">Related
     * StackOverflow answer</a>
     * @since 0.8.0
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
        log.debug("Project maven version is: " + version);
    }

    /**
     * Finds the project maven version, if not possible will fallback to "master"
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public String findVersion(String fallbackValue) {
        if (StringUtils.isEmpty(version)) {
            log.info("Not able to resolve project version, defaulting to: " + fallbackValue);
            return fallbackValue;
        } else {
            return version;
        }
    }

    private String normalizeVersion(String version) {
        return version == null ? version : version.replaceAll("-SNAPSHOT", "");

    }
}
