<?php namespace OwgeAccount\Service;
use OwgeAccount\Handler\DbHandler;
use OwgeAccount\Popo\Universe;
use OwgeAccount\Exception\RequiredElementNotFoundException;

/**
 * Service for handling universe operations
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class UniverseService {
    const MOCK_UNIVERSE_ID = 0;

    /** @var DbHandler */
    protected $dbHandler;

    public function __construct(DbHandler $dbHandler){
        $this->dbHandler = $dbHandler;
    }

    /**
    * Finds one universe
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function findOne(int $id): Universe {
        $result = $this->dbHandler->getConnection()->query("SELECT * FROM universes WHERE id=$id");
        return $result->num_rows
            ? $this->createUniverseFromRow($result->fetch_object(Universe::class))
            : null;
    }

    /**
    * Finds one universe or throws
    * 
    * @throws RequiredElementNotFoundException When element doesn't exists
    *
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function findOneOrThrow(int $id): Universe {
        $retVal = $this->findOne($id);
        if(!$retVal) {
            throw new RequiredElementNotFoundException("No universe with id $id was found");
        }
        return $this->createUniverseFromRow($retVal);
    }

    /**
    * Finds mock universe
    * 
    * @throws RequiredElementNotFoundException When element doesn't exists
    *
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function findMockUniverseOrThrow(): Universe {
        return $this->findOneOrThrow(self::MOCK_UNIVERSE_ID);
    }

    /**
    * Returns all the universes
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function findAll(): array {
        $retVal = [];
        $result = $this->dbHandler->getConnection()->query('SELECT * FROM universes');
        while($universe = $result->fetch_object(Universe::class)) {
            $retVal[] = $this->createUniverseFromRow($universe);
        }
        return $retVal;
    }

    /**
    * Updates the mock server backend urls
    * 
    * @since 0.8.0
    * @author Kevin Guanche Darias
    */
    public function updateMockUniverseUrls(string $backendUrl, string $frontendUrl): void {
        $connection = $this->dbHandler->getConnection();
        $backendUrl = $connection->real_escape_string($backendUrl);
        $frontendUrl = $connection->real_escape_string($frontendUrl);
        $connection->query("UPDATE universes SET rest_base_url = '$backendUrl', frontend_url = '$frontendUrl' WHERE id=" . self::MOCK_UNIVERSE_ID);
    }

    protected function createUniverseFromRow(Universe $universe): Universe {
        $universe->id = +$universe->id;
        $universe->creationDate = $universe->creation_date;
        $universe->restBaseUrl = $universe->rest_base_url;
        $universe->frontendUrl = $universe->frontend_url;
        $universe->targetDatabase = $universe->target_database;
        unset($universe->rest_base_url, $universe->frontend_url, $universe->target_database, $universe->creation_date);
        return $universe;
    }
}