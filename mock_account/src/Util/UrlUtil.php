<?php namespace OwgeAccount\Util;

/**
 * Has methods related with URLs
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
*/
class UrlUtil {
    /**
     * Checks if the URL is valid
     * 
     * @param string $url The URL
     * @param string $target The target of the validation (used to build the error message), for example "Invalid URL specified for $target"
     * 
     * @throws \InvalidArgumentException When the URL is not valid
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public static function checkUrl(string $url,string $target) {
        if(!\filter_var($url, \FILTER_VALIDATE_URL)) {
            throw new \InvalidArgumentException("Invalid URL specified for $target");
        }
    }

    private function __construct() {

    }
}