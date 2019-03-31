package com.kevinguanchedarias.owgejava.enumerations;

/**
 * Used to determine which type of mission can a specified unit <b>based in
 * planet ownership</b>, possible values are:
 * <ul>
 * <li><b>NONE:</b> The Unit can't execute the mission</li>
 * <li><b>OWNED_ONLY:</b> The unit can only do the mission to planets owned by
 * the user</li>
 * <li><b>ANY: The unit can run the mission in any planet</li>
 * </ul>
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public enum MissionSupportEnum {
	NONE, OWNED_ONLY, ANY;
}
