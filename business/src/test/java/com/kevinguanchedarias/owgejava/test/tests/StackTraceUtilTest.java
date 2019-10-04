/**
 * 
 */
package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.util.StackTraceUtil;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class StackTraceUtilTest {

	/**
	 * 
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Test
	public void shouldProperlyFormatStackTraceElement() {
		StackTraceElement stackTraceElement = new RuntimeException().getStackTrace()[0];
		int expectedLineNumber = stackTraceElement.getLineNumber();
		assertEquals(
				"com.kevinguanchedarias.owgejava.test.tests.StackTraceUtilTest.shouldProperlyFormatStackTraceElement:"
						+ expectedLineNumber,
				StackTraceUtil.formatTrace(stackTraceElement));
	}
}
