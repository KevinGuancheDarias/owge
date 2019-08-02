<?php namespace OwgeAccount\Command\Universe;

use OwgeAccount\Command\Base\OwgeCommand;
use OwgeAccount\Command\Base\OwgeListCommand;

/**
 * List the available universes
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
class UniverseListCommand extends OwgeListCommand {
    protected function getItemIdentifier(): string {
        return 'universe';
    }

    protected function getSelectQuery(): string {
        return 'SELECT id, name, rest_base_url, frontend_url FROM universes';
    }
}