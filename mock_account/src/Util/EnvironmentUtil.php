<?php namespace OwgeAccount\Util;

use OwgeAccount\Exception\BadEnvironmentException;

/**
 * Has utilities to help with Environment Variables
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class EnvironmentUtil {

    
    /**
     * Finds a environment variable or throws in case it has not been specified
     *
     * @param  mixed $envName Name of the environment variable
     * @return string value
     * 
     * @throws BadEnvironmentException
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public static function findOrThrow(string $envName): string {
        if(!$retVal = getenv($envName)) {
            throw new BadEnvironmentException("Missing required environment variable \e[36m$envName\e[33m");
        }
        return $retVal;
    }

    private function __construct() {

    }
}
