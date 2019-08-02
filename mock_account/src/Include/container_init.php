<?php

/**
 * @file Initializes the container by loading the specified config
 * 
 */
use DI\ContainerBuilder;

return (function() {
    $containerBuilder = new ContainerBuilder();
    $containerBuilder->addDefinitions(require_once(dirname(__FILE__) . '/../../config/services.php'));
    $container = $containerBuilder->build();
    return $container;
})();