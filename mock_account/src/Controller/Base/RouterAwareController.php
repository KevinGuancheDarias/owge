<?php

namespace OwgeAccount\Controller\Base;

use League\Route\Router;

/**
 * Controller that MUST have routes defined
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
*/
interface RouterAwareController {
    /**
     * Defines the routes that the controller should have
     */
    public function defineRoutes(Router $router): void;
}