/**
 * 
 */
package com.kevinguanchedarias.owgejava.enumerations;

/**
 * Identifies the currently used type of Deploy mission by the universe
 * <ul>
 * <li><b>FREEDOM:</b> You can do unlimited missions, default behavior before
 * v0.7.4</li>
 * <li><b>ONLY_ONCE_RETURN_DEPLOYED:</b> You can only deploy to one planet,
 * after that you have to either return, or run other mission, <b>After mission
 * your mission returns to the deployed planet</b></li>
 * <li><b>ONLY_ONCE_RETURN_SOURCE:</b> As ONLY_ONCE_RETURN_DEPLOYED but
 * instead.. returns to the original planet (the one the units was built
 * in)</li>
 * <li><b>DISALLOWED:</b> Doesn't not allow the deploy mission at all</li>
 * </ul>
 *
 * @since 0.7.4
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public enum DeployMissionConfigurationEnum {
	FREEDOM, ONLY_ONCE_RETURN_DEPLOYED, ONLY_ONCE_RETURN_SOURCE, DISALLOWED
}
