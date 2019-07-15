<?php namespace OwgeAccount\Command\Database;

use OwgeAccount\Command\Base\OwgeCommand;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;

/**
 * Detects if the DB is up
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
class DatabaseTestReadyCommand extends OwgeCommand {
    protected function configure() {
        $this->setName('owge:db:test_ready')
            ->setDescription('Test if the database is ready')
            ->setHelp('You do NOT really need help for using this command');
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        $connection = $this->getOwgeConnection();
        $result = $connection->query('SELECT id FROM universes LIMIT 1');
        if($result->num_rows) {
            $output->writeln('<info>DB is up</>');
            exit(0);
        } else {
            $output->writeln('<error>DB is NOT up for now... or has no universes (which should by default) </>');
        }

    }
}