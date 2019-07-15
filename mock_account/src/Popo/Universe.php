<?php namespace OwgeAccount\Popo;

class Universe {
    /** @var int */
    public $id;

    /** @var string */
    public $name; 

    /** @var string */
    public $description;

    /** @var \Datetime */
    public $creationDate;

    /** @var string */
    public $restBaseUrl;

    /** @var string */
    public $frontendUrl;

    /** @var string */
    public $targetDatabase;
}