package com.kevinguanchedarias.owgejava.test.helper;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

/**
 * This class contains methods useful to fake UserStorage operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UserMockitoHelper {

	private UserStorageBo userStorageBoMock;

	/**
	 * Constructor that accepts inserting the mock
	 * 
	 * @param target
	 *            Will inject the mock in this object
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UserMockitoHelper(Object target) {
		userStorageBoMock = Mockito.mock(UserStorageBo.class);
		Whitebox.setInternalState(target, "userStorageBo", userStorageBoMock);
	}

	/**
	 * Mocks the "logged in" feature
	 * 
	 * @param userStorageBoMock
	 *            Mock of the userStorageBo
	 * @param user
	 *            User to return when find logged in is called
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void fakeLoggedIn(UserStorage user) {
		Mockito.when(userStorageBoMock.findLoggedIn()).thenReturn(user);
	}

	public void testLoggedInInvoked() {
		Mockito.verify(userStorageBoMock, Mockito.times(1)).findLoggedIn();
	}

	/**
	 * Fake the user exists functionality
	 * 
	 * @param userId
	 *            id to fake
	 * @param user
	 *            User to return when searching for <i>id</i>
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void fakeUserExists(Integer userId, UserStorage user) {
		Mockito.when(userStorageBoMock.findById(userId)).thenReturn(user);
	}

	public UserStorageBo getUserStorageBoMock() {
		return userStorageBoMock;
	}

}
