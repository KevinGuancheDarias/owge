<?php
require_once dirname(__FILE__) . '/../vendor/autoload.php';

use Zend\Diactoros\ServerRequestFactory;
use Zend\Diactoros\ResponseFactory;
use League\Route\Strategy\JsonStrategy;
use League\Route\Router;
use Psr\Http\Message\ServerRequestInterface;
use Zend\HttpHandlerRunner\Emitter\SapiEmitter;

$request = ServerRequestFactory::fromGlobals(
    $_SERVER, $_GET, $_POST, $_COOKIE, $_FILES
);

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

$reponseFactory = new ResponseFactory;

$container = require_once dirname(__FILE__) . '/../src/Include/container_init.php';

$strategy = new JsonStrategy($reponseFactory);
$router = (new Router);
$router->setStrategy($strategy);
require_once(dirname(__FILE__) . '/../config/routes.php');

$router->get('/', function (ServerRequestInterface $request): array {
    return [
        'say' => 'Hello'
    ];
});

$response = $router->dispatch($request);

(new SapiEmitter)->emit($response);
