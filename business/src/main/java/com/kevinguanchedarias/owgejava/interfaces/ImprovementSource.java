/**
 * 
 */
package com.kevinguanchedarias.owgejava.interfaces;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;

/**
 * Tells the system, that class implementing this interface are able to detect
 * the current improvement values for a user
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface ImprovementSource {

	/**
	 * Returns the list of improvements that must be apply
	 * 
	 * @param user The user that has the improvements
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GroupedImprovement calculateImprovement(UserStorage user);
}
