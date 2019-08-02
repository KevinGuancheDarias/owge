<?php namespace OwgeAccount\Command\Universe;

use OwgeAccount\Command\Base\OwgeCommand;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputOption;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use OwgeAccount\Util\UrlUtil;
use OwgeAccount\Service\UniverseService;

/**
 * Changes the URLs for the mock universe
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
class UniverseChangeUrlsCommand extends OwgeCommand {

    protected function configure() {
        $this->setName('owge:universe:change_urls')
            ->setDescription('Changes the URLs of the backend and the frontend')
            ->addArgument('backendUrl', InputArgument::REQUIRED, 'URL of the backend')
            ->addArgument('frontendUrl', InputArgument::REQUIRED, 'URL of the frontend')
            ->addOption('yes', 'y', InputOption::VALUE_NONE, 'Automatically assume yes to all confirmations')
            ->setHelp('You do NOT really need help for using this command');
    }

    protected function execute(InputInterface $input, OutputInterface $output) {
        $backendUrl = $input->getArgument('backendUrl');
        $frontendUrl = $input->getArgument('frontendUrl');
        UrlUtil::checkUrl($backendUrl, 'backendUrl');
        UrlUtil::checkUrl($frontendUrl, 'frontendUrl');
        $this->getUniverseService()->updateMockUniverseUrls($backendUrl, $frontendUrl);
    }

    private function getUniverseService(): UniverseService {
        return $this->container->get(UniverseService::class);
    }
}