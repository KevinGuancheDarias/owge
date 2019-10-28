/**
 * 
 */
package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.builder.ExceptionBuilder;
import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.test.stub.TestBuilderException;
import com.kevinguanchedarias.owgejava.util.GitUtilService;

/**
 *
 * @todo In a far far away future, complete all those tests
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class ExceptionBuilderTest {

	@Test
	public void properlyAddsMessageAsDoc() {
		String message = "I18N_ERR_NOOB";
		GitUtilService gitUtilServiceMock = Mockito.mock(GitUtilService.class);
		Mockito.when(gitUtilServiceMock.createDocUrl(anyString(), any(GameProjectsEnum.class), any(Class.class),
				any(DocTypeEnum.class), anyString())).thenReturn("foo");
		CommonException exception = ExceptionBuilder.create(gitUtilServiceMock, TestBuilderException.class, message)
				.withDeveloperHintDoc(GameProjectsEnum.BUSINESS, ImprovementUnitTypeDto.class, DocTypeEnum.EXCEPTIONS)
				.build();
		assertEquals(message, exception.getMessage());
		assertTrue("Exception must be a instance of CommonException", exception instanceof CommonException);
		assertTrue("Exception must be a instance of SgtBackendInvalidInputException",
				exception instanceof TestBuilderException);
		assertEquals("foo", exception.getDeveloperHint());
	}
}
