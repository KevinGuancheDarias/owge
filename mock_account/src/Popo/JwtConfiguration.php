<?php namespace OwgeAccount\Popo;

use OwgeAccount\Exception\BadDatabaseConfigurationException;

/**
 * Represents the database configuration for JWT 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class JwtConfiguration {

    private const DEFAULT_DURATION = 3600 * 24 * 7;

    /** @var string */
    public $algo;

    /** @var int Duration in seconds */
    public $duration;

    /** @var string Secret */
    public $secret;

    /** @var array The raw passed data */
    private $rawData;

    /**
     * Creates an instance from a raw object
     *
     * @param  mixed $input
     * @return self
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public static function createFromRaw(array $input): self {
        self::checkParam($input, 'JWT_ALGO');
        self::checkParam($input, 'JWT_DURATION_SECONDS');
        self::checkParam($input, 'JWT_SECRET');
        $instance = self::createFromParams($input['JWT_ALGO'], $input['JWT_SECRET'], $input['JWT_DURATION_SECONDS']);
        $instance->rawData = $input;
        return $instance;
    }

    /**
     * createFromParams
     *
     * @param  mixed $algo
     * @param  mixed $secret
     * @param  mixed $duration
     * @return self
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public static function createFromParams(string $algo, string $secret, int $duration = self::DEFAULT_DURATION): self {
        $instance = new self();
        $instance->algo = $algo;
        $instance->secret = $secret;
        $instance->duration = $duration;
        return $instance;
    }

    protected static function checkParam(array $input, string $param): void {
        if(!isset($input[$param]) || !$input[$param]) {
            throw new BadDatabaseConfigurationException("Database configuration table doesn't have requried $param configuration, or computes to falsy");
        }
    }

    private function __construct(){
        
    }

    /**
     * 
     * 
     * @return array The raw data
     * 
     * @throws \InvalidArgumentException If instance was created without using createFromRaw()
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     *
     */
    public function getRawData(): array {
        if(!$this->rawData) {
            throw new \InvalidArgumentException("Can't invoke getRawData() when object " . get_class() . " has been created without using createFromRaw()");
        }
        return $this->rawData;
    }

}