<?php
use OwgeAccount\Command\User\UserListCommand;
use OwgeAccount\Command\User\UserCreateCommand;
use OwgeAccount\Command\User\UserDeleteCommand;
use OwgeAccount\Command\Universe\UniverseListCommand;
use OwgeAccount\Command\Universe\UniverseChangeUrlsCommand;
use OwgeAccount\Command\Database\DatabaseTestReadyCommand;

return [
    new UserListCommand,
    new UserCreateCommand,
    new UserDeleteCommand,
    new UniverseListCommand,
    new UniverseChangeUrlsCommand,
    new DatabaseTestReadyCommand
];