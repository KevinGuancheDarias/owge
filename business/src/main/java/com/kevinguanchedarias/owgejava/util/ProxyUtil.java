/**
 *
 */
package com.kevinguanchedarias.owgejava.util;

import java.util.function.Consumer;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ProxyUtil implements ApplicationContextAware {

	private static BeanFactory beanFactory;

	/**
	 *
	 * @param <T>
	 * @param beanClass
	 * @return
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <T> T getBean(Class<T> beanClass) {
		return beanFactory.getBean(beanClass);
	}

	/**
	 *
	 * @param <T>
	 * @param sprinbBeanClass
	 * @param action
	 * @param ifTransactionAction Action to run when we are already in a transaction
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <T> void runProxiedIfNotTransactional(Class<T> sprinbBeanClass, Consumer<T> action,
			Runnable ifTransactionAction) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()) {
			action.accept(getBean(sprinbBeanClass));
		} else if (ifTransactionAction != null) {
			ifTransactionAction.run();
		}
	}

	/**
	 *
	 * @param <T>
	 * @param sprinbBeanClass
	 * @param action
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <T> void runProxiedIfNotTransactional(Class<T> sprinbBeanClass, Consumer<T> action) {
		runProxiedIfNotTransactional(sprinbBeanClass, action, null);
	}

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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		beanFactory = applicationContext;
	}
}
