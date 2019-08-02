<?php namespace OwgeAccount\Command\Base;

use Symfony\Component\Console\Command\Command;
use DI\Container;
use OwgeAccount\Handler\DbHandler;
use Symfony\Component\Console\Helper\QuestionHelper;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Question\Question;
use Symfony\Component\Console\Question\ConfirmationQuestion;

/**
 * Represents the require base class for the commands
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
abstract class OwgeCommand extends Command {
    /** @var Container */
    protected $container;

    /**
     * Defines the App Container
     *
     * @param  Container $container
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */
    public function setContainer(Container $container): void {
        $this->container = $container;
    }

    /**
     * Gets the owge connection
     *
     * @return \mysqli The connection
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */
    protected function getOwgeConnection(): \mysqli {
        return $this->container->get(DbHandler::class)->getConnection();
    }

    /**
     * Gets the question helper
     *
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */
    protected function getQuestionHelper (): QuestionHelper {
        return $helper = $this->getHelper('question');
    }


    /**
     * Finds an option or prompts for it
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */
    protected function findOptionValueOrPrompt(InputInterface $input, OutputInterface $output, string $optionName, string $defaultValue) {
        $value = $input->getOption($optionName);
        if(!$value) {
            $value = $this->getQuestionHelper()->ask($input, $output, new Question("Please insert value for $optionName: [<comment>$defaultValue</>] ", $defaultValue));
        }
        return $value;
    }

    /**
     * Returns true if yes option is passed, if yes option is false will prompt user for confirmation
     *
     * @param InputInterface $input
     * @param OutputInterface $output
     * @param string $confirmText Text used when prompting the user
     * 
     * @return bool
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
    */
    protected function yesOptionOrConfirm(InputInterface $input, OutputInterface $output, string $confirmText): bool {
        return ($input->getOption('yes') 
                || $this->getQuestionHelper()->ask($input, $output, new ConfirmationQuestion($confirmText)));
    }
}