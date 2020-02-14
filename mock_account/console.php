#!/usr/bin/env php
<?php
use Symfony\Component\Console\Application;
use OwgeAccount\Command\Base\OwgeCommand;
use Symfony\Component\Console\Output\ConsoleOutput;
use Symfony\Component\Console\Question\Question;
use Symfony\Component\Console\Input\ArrayInput;
use Symfony\Component\Console\Input\StringInput;

require_once dirname(__FILE__) . '/vendor/autoload.php';
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

if( php_sapi_name() !== 'cli' ) {
    exit(0);
}
try {
    if(!getenv('OWGE_IGNORE_WARNINGS')) {
        echo "\e[33mWarning: For development purpose. Do \e[35mNOT\e[33m use Mock account in production, configure an actual Oauth2 RSA implementation\e[39m" . \PHP_EOL;
    }
    $container = require_once dirname(__FILE__) . '/src/Include/container_init.php';
    $app = new Application('OWGE Mock account system :: Kevin Guanche Darias & contributors');
    $app->setAutoExit(false);
    $commands = require_once dirname(__FILE__) . '/config/commands.php';
    if(!is_array($commands)) {
        throw new \InvalidArgumentException('commands.php file MUST return an array of commands');
    }
    foreach($commands as $command) {
        if(!($command instanceof OwgeCommand)) {
            throw new \InvalidArgumentException('command: ' . get_class($command) . ' is not an instance of OwgeCommand, bad configuration in commands.php');
        }
        $command->setContainer($container);
        $app->add($command);
    }

    $firstTime = true;
    while(true) {
        if(!getenv('OWGE_INTERACTIVE')) {
            $exitCode = $app->run();
            exit($exitCode);
        } else {
            $output = new ConsoleOutput;
            if($firstTime) {
                $app->find('list')->run(new ArrayInput([]), $output);
            }
            $command = $app->getHelperSet()->get('question')->ask(new ArrayInput([]), $output , new Question('Insert command (write <comment>exit</> to quit): '));
            $input = new StringInput($command);
            if($command === 'exit') {
                exit;
            }
            $app->run($input, $output);
            $firstTime = false;
        }
    }
}catch (\Exception $e ) {
    fwrite(STDERR, "\e[31mUnexpected fatal error: \e[35mHint: \e[33m{$e->getMessage()}\e[39m" . \PHP_EOL);
}