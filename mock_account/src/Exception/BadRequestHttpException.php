<?php namespace OwgeAccount\Exception;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class BadRequestHttpException extends HttpException {
    public function __construct(string $errorType = 'bad_request', string $message = 'Bad Request') {
        parent::__construct($errorType, 400, $message);
    }
}