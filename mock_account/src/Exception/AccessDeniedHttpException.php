<?php namespace OwgeAccount\Exception;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class AccessDeniedHttpException extends HttpException {
    public function __construct(string $message = 'Forbidden') {
        parent::__construct(403, $message);
    }
}