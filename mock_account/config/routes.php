<?php
use OwgeAccount\Handler\RouterConfigHandler;
use OwgeAccount\Controller\UserController;
use OwgeAccount\Controller\UniverseController;

if(!isset($router)) {
    throw new \InvalidArgumentException('You can NOT use this file without defining $router');
}
$routerConfigHandler = new RouterConfigHandler($router);

$routerConfigHandler->addControllers(
    $container->get(UserController::class),
    $container->get(UniverseController::class)
);