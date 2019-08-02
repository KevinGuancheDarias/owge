<?php namespace OwgeAccount\Controller;
use OwgeAccount\Controller\Base\RouterAwareController;
use League\Route\Router;
use OwgeAccount\Service\UniverseService;
use League\Route\RouteGroup;

/**
 * Controller for handling universe operations
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class UniverseController implements RouterAwareController {

    /** @var UniverseService */
    protected $universeService;

    public function __construct(UniverseService $universeService) {
        $this->universeService = $universeService;
    }
    public function defineRoutes(Router $router): void {
        $router->group('/universe', function (RouteGroup $routeGroup) {
            $routeGroup->get('findOfficials', [$this, 'findOfficials']);
        });
    }

    /**
    * Finds <b>ALL</b> universes, despite the "officials", this is a mock implementation, and doesn't have the concept of universe type
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function findOfficials(): array {
        return $this->universeService->findAll();
    }
}