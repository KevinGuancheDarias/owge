<?php

namespace OwgeAccount\Controller\Base;

use OwgeAccount\Handler\DbHandler;

/**
 * Controller that MUST have DBHandler
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
*/
interface DbHandlerAwareController {
    public function setDbHandler(DbHandler $dbHandler);
}