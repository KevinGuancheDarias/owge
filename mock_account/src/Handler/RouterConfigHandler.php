<?php

namespace OwgeAccount\Handler;

use OwgeAccount\Controller\Base\RouterAwareController;
use League\Route\Router;

/**
 * Handles the configuration
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class RouterConfigHandler {
    /** @var Router */
    protected $router;

    public function __construct( Router $router) {
        $this->router = $router;
    }

    /**
     * addControllers
     *
     * @param  array $controllers that defines routes (RouterAwareController)
     *
     * @return void
     */
    public function addControllers(RouterAwareController ...$controllers): void {
        foreach ($controllers as $controller) {
            $controller->defineRoutes($this->router);
        }
    }
}