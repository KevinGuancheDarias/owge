<?php namespace OwgeAccount\Command\Base;

use Symfony\Component\Console\Helper\Table;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Input\InputInterface;

/**
 * Commands displaying a list of something should extend this class
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
abstract class OwgeListCommand extends OwgeCommand {

    /**
     * Identifier of the item to use
     * 
     * @return string The item identifier
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */    
    abstract protected function getItemIdentifier(): string;

    /**
     * SQL SELECT query to use
     * 
     * @return string The SQL query
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */    
    abstract protected function getSelectQuery(): string;

    protected function configure() {
        $identifier = $this->getItemIdentifier();
        $this->setName("owge:$identifier:list")
            ->setDescription("List {$identifier}s")
            ->setHelp('You do NOT really need help for using this command');
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        $data = $this->getOwgeConnection()->query($this->getSelectQuery());
        if($data->num_rows) {
            $this->renderListTable($data->fetch_all(\MYSQLI_ASSOC), $output);
        } else {
            $output->writeln("<fg=yellow>No {$this->getItemIdentifier()}s found</>");
        }
    }

    protected function renderListTable(array $data, OutputInterface $output): void {
        $table = new Table($output);
        $keys = array_keys($data[0]);
        $table->setHeaders($keys);
        $table->setRows(array_map(function($entry) use ($keys){
            $retVal = [];
            foreach($keys as $currentKey) {
                $retVal[] = $entry[$currentKey];
            }
            return $retVal;
        }, $data));
        $table->render();
    }
}