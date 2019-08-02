<?php namespace OwgeAccount\Util;

use Psr\Http\Message\ServerRequestInterface;
use OwgeAccount\Exception\BadRequestHttpException;

/**
 * Has methods related with the request
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
*/
class RequestUtil {
    
    
    /**
     * Check POST formdata
     *
     * @param  ServerRequestInterface $request
     * @param  callable $validationFunction Function to use to validate the param
     * @param  mixed $params
     *
     * @return array
     */
    public static function checkFormInput(ServerRequestInterface $request, callable $validationFunction, string ...$params): array {
        return self::checkMultiple($request, 'form-data', $validationFunction, ...$params);
    }

    /**
     * checkFormInput
     *
     * @param  ServerRequestInterface $request
     * @param  string $typeName  Type name for example "form-data", or "path", or "query"
     * @param  callable $validationFunction Function to use to validate the param
     * @param  string $params Params to validate
     *
     * @return array All the request params
     * 
     * @throws BadRequestHttpException When the param is missing
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public static function checkMultiple(ServerRequestInterface $request, string $typeName, callable $validationFunction, string ...$params) : array{
        $body = $request->getParsedBody();
        foreach($params as $param) {
            self::checkSingle($body, $param, $typeName, $validationFunction);
        }
        return $body;
    }

    /**
     * Checks single input value
     *
     * @param  array $input
     * @param  mixed $key Param to search
     * @param  mixed $typeName  Type name for example "form-data", or "path", or "query"
     * @param  mixed $validationFunction Function to use to validate the param
     * 
     * @return mixed the value of the param
     * 
     * @throws BadRequestHttpException When the param is missing
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias 
     */
    public static function checkSingle(array $input, string $key, string $typeName, callable $validationFunction) {
        if(!isset($input[$key])) {
            throw new BadRequestHttpException("Missing $typeName param with name $key in the request");
        } else if(!$validationFunction($input[$key])) {
            throw new BadRequestHttpException("Specified value for $key is not valid ");
        }
    }

    private function __construct() {

    }
}
