<?php namespace OwgeAccount\Command\User;

use OwgeAccount\Command\Base\OwgeCommand;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Question\ConfirmationQuestion;
use DateTime;
use OwgeAccount\Service\UserService;

/**
 * Creates a new user
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
class UserCreateCommand extends OwgeCommand {
    protected function configure() {
        $this->setName('owge:user:create')
            ->setDescription('Create user account')
            ->addOption('username','u', InputOption::VALUE_OPTIONAL)
            ->addOption('email', 'e', InputOption::VALUE_OPTIONAL)
            ->addOption('password', 'p', InputOption::VALUE_OPTIONAL)
            ->addOption('yes', 'y', InputOption::VALUE_NONE, 'Automatically assume yes to all confirmations')
            ->setHelp('You do NOT really need help for using this command');
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        $connection = $this->getOwgeConnection();
        $suggestedUser = $this->findSuggestedUserInfo();

        $username = $this->findOptionValueOrPrompt($input,$output, 'username', $suggestedUser['username']);
        $email = $this->findOptionValueOrPrompt($input,$output, 'email', $suggestedUser['email']);
        $password = $this->findOptionValueOrPrompt($input,$output, 'password', $suggestedUser['password']);

        if(!$input->getOption('yes') 
                && !$this->getQuestionHelper()->ask($input, $output, new ConfirmationQuestion("Create user $username, with email $email, and password $password? "))) {
            $output->writeln('<comment>did nothing</>');
        } else {
            $this->container->get(UserService::class)->createUser([
                'email' => $email,
                'username' => $username,
                'password' => $password
            ]);
            $output->writeln('<info>Done</>');
        }
        
    }

    protected function findSuggestedUserInfo() {
        $userNum = 0;
        do {
            $userNum++;
        } while($this->getOwgeConnection()->query("SELECT username FROM users WHERE username = 'user_$userNum'")->num_rows);
        return [
            'username' => "user_$userNum",
            'email' => "user_$userNum@example.com",
            'password' => '1234'
        ];
    }
}