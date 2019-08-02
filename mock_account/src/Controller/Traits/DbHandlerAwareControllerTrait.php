<?php

namespace OwgeAccount\Controller\Traits;

use OwgeAccount\Handler\DbHandler;

/**
 * Defines the DBHandler
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
trait DbHandlerAwareControllerTrait {
    /** @var DbHandler */
    protected $dbHandler;

    /**
    * Sets the DBHandler
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function setDbHandler(DbHandler $dbHandler) {
        $this->dbHandler = $dbHandler;
    }
}