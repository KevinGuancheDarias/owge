<?php namespace OwgeAccount\Command\User;

use OwgeAccount\Command\Base\OwgeCommand;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Question\ConfirmationQuestion;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Input\ArrayInput;

class UserDeleteCommand extends OwgeCommand {
    protected function configure() {
        $this->setName('owge:user:delete')
            ->setDescription('Delete user account')
            ->addArgument('id', InputArgument::REQUIRED, 'Id of the user')
            ->addOption('yes', 'y', InputOption::VALUE_NONE, 'Automatically assume yes to all confirmations')
            ->setHelp('You do NOT really need help for using this command');
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        $connection = $this->getOwgeConnection();
        if(!\is_numeric($input->getArgument('id'))) {
            throw new \InvalidArgumentException('id must be numeric');
        }
        $id = +$input->getArgument('id');
        $user = $connection->query("SELECT username FROM users WHERE id = $id");
        if($user->num_rows) {
            $username = $user->fetch_object()->username;
            if($this->yesOptionOrConfirm($input, $output, "Delete user with id $id and username $username?: ")) {
                $connection->query("DELETE FROM users WHERE id = $id");
                $output->writeln('<info>Done</>');
            }
        } else {
            $output->writeln('<comment>Nothing to do, no such user</>');
            if($this->yesOptionOrConfirm($input, $output, "Display users? ")) {
                $this->getApplication()->find('owge:user:list')->run(new ArrayInput([]), $output);
            }
        }
    }
}