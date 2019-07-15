<?php namespace OwgeAccount\Handler;

use OwgeAccount\Util\EnvironmentUtil;
use OwgeAccount\Popo\JwtConfiguration;
use OwgeAccount\Exception\BadDatabaseConfigurationException;

/**
 * Class for handling all things related to db
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class DbHandler{

    private const FIND_CONFIG_QUERY = "SELECT  name, value FROM configuration WHERE name IN ('JWT_ALGO', 'JWT_DURATION_SECONDS', 'JWT_SECRET')";

    /** @var \mysqli */
    private $connection;

    /**
     *
     * @param  bool $connect Connect to database on construct, defaults to true
     * @param bool $checkConfig Checks mysql configuration table, to find if required properties exists in that table, defaults to true, ignored if $connect is false
     * 
     * @throws \OwgeAccount\Exception\BadEnvironmentException If connection failed due to missing env vars
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function __construct(bool $connect = true, bool $checkConfig = true) {
        if($connect) {
            $this->connect();
            if($checkConfig) {
                $this->checkConfig();
            }
        }
    }

    
    /**
     * Connect to the database
     *
     * @return self
     * @throws \OwgeAccount\Exception\BadEnvironmentException If connection failed due to missing env vars
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     * 
     */
    public function connect(): self {
        $host = getenv('MYSQL_HOST') ?? '127.0.0.1';
        $port = getenv('MYSQL_PORT') ?? '3306';
        $db = EnvironmentUtil::findOrThrow('MYSQL_DB');
        $user = EnvironmentUtil::findOrThrow('MYSQL_USER');
        $pass = EnvironmentUtil::findOrThrow('MYSQL_PASSWORD');
        $this->connection = new \mysqli($host, $user, $pass, $db, $port);
        return $this;
    }


    /**
     * Checks mysql configuration table, to find if required properties exists in that table
     *
     * @return self
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function checkConfig(): self {
        $result = $this->connection->query(self::FIND_CONFIG_QUERY);
        $this->parseConfigRowResultsOrThrow($result);
        return $this;
    }

    
    /**
     * Finds the current configuration to use for JWT
     *
     * @return JwtConfiguration
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
    */
    public function findConfig(): JwtConfiguration {
        $result = $this->connection->query(self::FIND_CONFIG_QUERY);
        return $this->parseConfigRowResults($result);
    }

    /**
     * Gets the database connection
     * 
     * @return \mysqli
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function getConnection(): \mysqli {
        return $this->connection;
    }

    private function parseConfigRowResults(\mysqli_result $result): JwtConfiguration {
        $values = [];
        while($row = $result->fetch_object()) {
            $values[$row->name] = $row->value;
        }
        return JwtConfiguration::createFromRaw($values);
    }

    private function parseConfigRowResultsOrThrow(\mysqli_result $result): JwtConfiguration {
        $jwtConfiguration = $this->parseConfigRowResults($result);
        $values = $jwtConfiguration->getRawData();
        foreach(['JWT_ALGO', 'JWT_DURATION_SECONDS', 'JWT_SECRET'] as $key) {
            if(!isset($values[$key]) || !$values[$key]) {
                throw new BadDatabaseConfigurationException("Database Configuration table doesn't have the required $key, or it's falsy");
            }
        }
        return $jwtConfiguration;
    }
}
