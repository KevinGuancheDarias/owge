package com.kevinguanchedarias.owgejava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.pojo.WebsocketMessage;

@RunWith(BlockJUnit4ClassRunner.class)
public class WebsocketMessageTest {
	private static final String PROTOCOL_VERSION = "0.1.0";

	@Test(expected = ProgrammingException.class)
	public void shouldThrowWhenInvalidStatusSpecified() {
		new WebsocketMessage("some", PROTOCOL_VERSION, "the_random");
	}

	@Test
	public void shouldAllowOkAndErrorStatus() {
		new WebsocketMessage("something", PROTOCOL_VERSION, "ok");
		new WebsocketMessage("something", PROTOCOL_VERSION, "error");
	}
}
