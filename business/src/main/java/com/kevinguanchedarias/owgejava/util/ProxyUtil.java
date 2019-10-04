/**
 * 
 */
package com.kevinguanchedarias.owgejava.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ProxyUtil {

	/**
	 * Resolves the specified object if possible to his real class
	 * 
	 * @param clazz
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static Class<?> resolveProxy(Object clazz) {
		if (AopUtils.isAopProxy(clazz)) {
			Advised springProxy = (Advised) clazz;
			return springProxy.getProxiedInterfaces()[0];
		} else {
			return (Class<?>) clazz;
		}
	}

	private ProxyUtil() {
		// An util class can't have instance
	}
}
