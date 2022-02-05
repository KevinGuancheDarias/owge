package com.kevinguanchedarias.owgejava.exception;

import java.io.Serial;

/**
 * This exception is thrown when tried to save an entity but that has been
 * deleted by other process
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.19
 */
public class OwgeElementSideDeletedException extends CommonException {
    @Serial
    private static final long serialVersionUID = -4314686177498588774L;

    public OwgeElementSideDeletedException(String message) {
        super(message);
    }

}
