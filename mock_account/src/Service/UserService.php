<?php namespace OwgeAccount\Service;

use OwgeAccount\Handler\DbHandler;
use Jose\Component\Signature\JWSBuilder;
use Jose\Component\KeyManagement\JWKFactory;
use Jose\Component\Core\AlgorithmManager;
use Jose\Component\Signature\Serializer\CompactSerializer;
use OwgeAccount\Exception\AccessDeniedHttpException;
use OwgeAccount\Exception\BadRequestHttpException;
use OwgeAccount\Popo\LoggedUser;
use Jose\Component\Core\Util\JsonConverter;
use Jose\Component\Signature\Algorithm\HS256;

/**
 * Represents the database configuration for user 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */

class UserService {
    /** @var DbHandler */
    protected $dbHandler;

    public function __construct(DbHandler $dbHandler){
        $this->dbHandler = $dbHandler;
    }

    /**
     * 
     * @return LoggedUser the JWT token
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function login(string $email, string $password): LoggedUser {
        $connection = $this->dbHandler->getConnection();
        $email = $connection->real_escape_string($email);
        $password = $connection->real_escape_string($password);
        $result = $connection->query("SELECT id, username, email, password FROM users WHERE email = '$email' AND password = '$password'");
        if(!$result->num_rows) {
            throw new AccessDeniedHttpException('invalid_credentials','Invalid credentials');
        }
        $user = $result->fetch_object(LoggedUser::class);
        $user->token = $this->createToken(+$user->id, $user->email, $user->username);;
        return $user;
    }

    /**
     * Returns all the users, note only returns the username and the id
     *  
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function findAll(): array {
        $connection = $this->dbHandler->getConnection();
        $result = $connection->query('SELECT id, username FROM users');
        return array_map(function($current) {
            $current['id'] = +$current['id'];
            return $current;
        },$result->fetch_all(MYSQLI_ASSOC));
    }

    /**
     * 
     * Creates the user into the database
     * 
     * @param array $userData The user information, requires: email, username and password
     * 
     * @since 0.8.0
     * @author Kevin Guanche Darias
     */
    public function createUser(array $userData): void {
        if(!isset($userData['email'],$userData['username'], $userData['password']) || !$userData['email'] || !$userData['username'] || !$userData['password']) {
            throw new BadRequestHttpException('Not all the required parameters has been submit, or they are falsy, required: email, username, password');
        }
        $connection = $this->dbHandler->getConnection();
        $email = $connection->real_escape_string($userData['email']);
        $username = $connection->real_escape_string($userData['username']);
        $password = $connection->real_escape_string($userData['password']);
        $creationDate = $this->toMysqlDate();
        $lastLogin = $this->toMysqlDate();
        $connection->query("
            INSERT INTO users (username, email, password, creation_date, last_login) 
                        VALUES ('$username', '$email', '$password', '$creationDate', '$lastLogin')
        ");
    }

    protected function createToken(int $userId, string $email, string $username): string {
        $jsonConverter = new JsonConverter();
        $config = $this->dbHandler->findConfig();
        $alg = $this->dbHandler->findConfig()->algo;
        $jwk = JWKFactory::createFromSecret(
            $config->secret, [ 
                'alg' => $alg,
                'use' => 'sig'
            ]
        );

        $payload = $jsonConverter->encode([
            'iat' => time(),
            'nbf' => time(),
            'exp' => time() + 3600,
            'sub' => $userId,
            'data' => [
                'id' => $userId,
                'username' => $username,
                'email' => $email
            ]

        ]);
        
        $algorithmManager = new AlgorithmManager([
            new HS256(),
        ]);

        $jwsBuilder = new JWSBuilder(
            $algorithmManager
        );

        $jws = $jwsBuilder->create()->withPayload($payload)->addSignature($jwk, ['alg' => $config->algo])->build();
        return (new CompactSerializer($jsonConverter))->serialize($jws);
    }

    private function toMysqlDate(?int $time = null): string {
        return (new \DateTime('@' . ($time ?? time())))->format('Y-m-d H:i:s');
    }
}
?>