<?php

namespace OwgeAccount\Controller;

use League\Route\Router;
use OwgeAccount\Controller\Base\RouterAwareController;
use League\Route\RouteGroup;
use Psr\Http\Message\ServerRequestInterface;
use OwgeAccount\Controller\Traits\DbHandlerAwareControllerTrait;
use OwgeAccount\Service\UserService;
use OwgeAccount\Util\RequestUtil;

/**
 * Controller for handling user operations
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class UserController implements RouterAwareController {
    use DbHandlerAwareControllerTrait;

    /** @var UserService */
    private $userService;

    public function __construct(UserService $userService) {
        $this->userService = $userService;
    }

    public function defineRoutes(Router $router): void {
        $router->group('/user', function (RouteGroup $routeGroup) {
            $routeGroup->post('login',[$this, 'login']);
        });
    }

    /**
    * Logins to the game
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function login(ServerRequestInterface $request): array  {
        $data = RequestUtil::checkFormInput($request, 'is_string', 'username', 'password');
        $userData = $this->userService->login($data['username'],$data['password']);
        return [
            'token' => $userData->token,
            'userId' => $userData->id
        ];
    }
}