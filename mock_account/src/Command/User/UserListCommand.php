<?php namespace OwgeAccount\Command\User;

use OwgeAccount\Command\Base\OwgeListCommand;

/**
 * List the available users
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
class UserListCommand extends OwgeListCommand {
    protected function getItemIdentifier(): string {
        return 'user';
    }

    protected function getSelectQuery(): string {
        return 'SELECT `id`,`username`,`password`,`email` FROM users';
    }
}