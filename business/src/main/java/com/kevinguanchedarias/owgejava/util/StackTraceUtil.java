package com.kevinguanchedarias.owgejava.util;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class StackTraceUtil {

	/**
	 * Formats the stack trace Element to display a readable short string
	 * 
	 * @param stackTraceElement
	 * @return readable short string
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static String formatTrace(StackTraceElement stackTraceElement) {
		return "" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + ":"
				+ stackTraceElement.getLineNumber();
	}

	private StackTraceUtil() {

	}
}
