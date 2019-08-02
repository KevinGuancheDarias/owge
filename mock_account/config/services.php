<?php
use OwgeAccount\Controller\UserController;
use function DI\autowire;
use OwgeAccount\Handler\DbHandler;
use function DI\create;
use OwgeAccount\Service\UniverseService;
use OwgeAccount\Controller\UniverseController;

return [
    DbHandler::class => create(),
    UserService::class => autowire(),
    UserController::class => autowire(),
    UniverseService::class => autowire(),
    UniverseController::class => autowire()
];
?>