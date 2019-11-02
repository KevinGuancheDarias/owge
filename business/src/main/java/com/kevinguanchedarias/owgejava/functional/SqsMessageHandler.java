/**
 * 
 */
package com.kevinguanchedarias.owgejava.functional;

import com.kevinguanchedarias.owgejava.pojo.OwgeSqsMessage;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@FunctionalInterface
public interface SqsMessageHandler {

	/**
	 * Handler action
	 * 
	 * @todo In the future, when SQS client supports it, return the ACK state
	 *       (boolean)
	 * @param message
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void handle(OwgeSqsMessage message);
}
