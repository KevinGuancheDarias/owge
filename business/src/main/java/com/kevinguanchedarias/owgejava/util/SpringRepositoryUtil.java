/**
 * 
 */
package com.kevinguanchedarias.owgejava.util;

import org.apache.log4j.Logger;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.repository.GameJpaRepository;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SpringRepositoryUtil {
	private static final Logger LOG = Logger.getLogger(SpringRepositoryUtil.class);
	private static final String COMMON_MISSING_AUTOWIRED_STRING = "please note that JpaRepositories injected with @Autowire are proxies";

	@SuppressWarnings("unchecked")
	public static Class<?> findEntityClass(Object repository) {
		if (AopUtils.isAopProxy(repository)) {
			Advised springProxy = (Advised) repository;
			Object target;
			try {
				target = springProxy.getTargetSource().getTarget();
				if (!GameJpaRepository.class.isAssignableFrom((target.getClass()))) {
					throw new ProgrammingException(
							"Passed object is not a spring repository, " + COMMON_MISSING_AUTOWIRED_STRING);
				}
				GameJpaRepository<Object, Number> castedRepository = (GameJpaRepository<Object, Number>) target;
				return castedRepository.findEntityClass();
			} catch (Exception e) {
				LOG.warn("Unable to resolve the proxy target", e);
				return null;
			}
		} else {
			throw new ProgrammingException(
					"You MUST pass a repository proxy instance, " + COMMON_MISSING_AUTOWIRED_STRING);
		}
	}

	private SpringRepositoryUtil() {
		// An util class doesn't have instance
	}
}
