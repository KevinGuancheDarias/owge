/**
 *
 */
package com.kevinguanchedarias.owgejava.enumerations;

/**
 * Has the possible values for document type inside the Git docs
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public enum DocTypeEnum {
    EXCEPTIONS, RESERVED;

    /**
     * Finds the path for the given enum type
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public String findPath() {
        return name().toLowerCase();
    }
}
