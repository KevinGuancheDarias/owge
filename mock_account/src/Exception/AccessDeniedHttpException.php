<?php namespace OwgeAccount\Exception;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class AccessDeniedHttpException extends HttpException {
    public function __construct(string $errorType, string $message = 'Forbidden') {
        parent::__construct($errorType, 403, $message);
    }
}