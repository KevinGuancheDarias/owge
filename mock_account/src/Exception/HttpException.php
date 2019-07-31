<?php namespace OwgeAccount\Exception;

use Psr\Http\Message\ResponseInterface;

/**
 * Also ensures the HTTP Exception errors are displayed like in KGDW accounts system
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class HttpException extends \League\Route\Http\Exception {
    /** @var string  */
    protected $errorType;

    public function __construct(string $errorType = '', int $code = 500, string $message = '') {
        $this->errorType = $errorType;
        if(!$message) {
            $message = $this->createErrorMessage($code);
        }
        parent::__construct($code, $message);
    }

    /**
     * {@inheritdoc}
     */
    public function buildJsonResponse(ResponseInterface $response): ResponseInterface {
        $response = parent::buildJsonResponse($response);
        if ($response->getBody()->isWritable()) {
            $response->getBody()->seek(0);
            $response->getBody()->write(json_encode([
                'error'   => $this->errorType,
                'message' => $this->message
            ]));
        }
        return $response;
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