/**
 * 
 */
package com.kevinguanchedarias.owgejava.enumerations;

/**
 * Has the possible message types that the Game recognizes<br>
 * <b>NOTICE:</b> The special NOT_KNOWN is used when can't determine the message
 * type
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public enum OwgeSqsMessageEnum {
	TIME_SPECIAL_EFFECT_END, TIME_SPECIAL_IS_READY, NOT_KNOWN;
}
