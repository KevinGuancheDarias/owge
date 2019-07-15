<?php namespace OwgeAccount\Exception;

/**
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class HttpException extends \Exception {
    public function __construct(int $code = 500, string $message = '') {
        if(!$message) {
            $message = $this->createErrorMessage($code);
        }
        parent::__construct($message, $code);
    }

    protected function createErrorMessage(int $code): string {
        if($code === 403) {
            return 'Forbidden';
        } else if ($code === 404) {
            return 'Not Found';
        } else if($code >= 500 && $code < 600) {
            return 'Server Error';
        } else {
            throw new \InvalidArgumentException('The specified HTTP code is not valid, or it is NOT supported by the automessage, you will have to manually define exception message');
        }
    }
}