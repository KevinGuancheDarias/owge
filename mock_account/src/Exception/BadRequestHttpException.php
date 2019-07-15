<?php namespace OwgeAccount\Exception;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class BadRequestHttpException extends HttpException {
    public function __construct(string $message = 'Bad Request') {
        parent::__construct(400, $message);
    }
}