package com.kevinguanchedarias.owgejava.exception;


import java.io.Serial;

public class ProgrammingException extends CommonException {
    @Serial
    private static final long serialVersionUID = -7298435329894498401L;

    /**
     * Default constructor for {@link ProgrammingException}, <br>
     * <b>As of 0.8.0 logs the exception</b>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public ProgrammingException(String message) {
        super(message);
    }

}
