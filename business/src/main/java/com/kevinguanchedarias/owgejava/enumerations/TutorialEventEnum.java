package com.kevinguanchedarias.owgejava.enumerations;

import com.kevinguanchedarias.owgejava.entity.TutorialSectionEntry;

/**
 * Represents the possible events for a {@link TutorialSectionEntry}
 *
 * <ul>
 * <li><b>CLICK:</b> Stops when the specified <i>htmlSymbol</i> is clicked</li>
 * <li><b>ANY_KEY_OR_CLICK:</b> Stops when any key or click/touch is
 * triggered</li>
 * </ul>
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public enum TutorialEventEnum {
	CLICK, ANY_KEY_OR_CLICK;
}
