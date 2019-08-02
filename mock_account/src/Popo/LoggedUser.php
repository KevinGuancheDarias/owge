<?php namespace OwgeAccount\Popo;

/**
 * Represents a logged in user
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias
 */
class LoggedUser {
    /** @var string */
    public $token;

    /** @var int */
    public $id;

    /** @var string */
    public $email;

    /** @var string */
    public $username;
}