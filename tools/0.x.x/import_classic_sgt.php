#!/usr/bin/env php
<?php
define('DYNAMIC_EXTRA_SOURCE', 'dynamic_extra');
define('TARGET_CLASSIC_IMAGES_STORAGE', 'owge_classic_media');
define('FACTION_INITIAL_ENERGY', 4200);
define('FACTION_ENERGY_ICON','owge_classic.png');
define('FACTION_ENERGY_NAME', 'Energy');
define('FACTION_MAX_PLANETS', 4);
define('TROOPS_TYPE_ID', 0);
define('SHIPS_TYPE_ID', 100);
define('DEFENSES_TYPE_ID', 1000);
define('CLASSIC_OWGE_DB_DELIMITER', ';');
define('DEFAULT_EXTENSION', '.jpg');
define('NG_BEEN_RACE_REQUIREMENT_CODE', 'BEEN_RACE');
define('NG_HAVE_UPGRADE_LEVEL_REQUIREMENT_CODE', 'UPGRADE_LEVEL');
define('NG_SOLDIERS_UNIT_TYPE_NAME', 'Tropas');
define('NG_SOLDIERS_LIMIT', 10);

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

echo 'Kevin Guanche Darias :: OWGE Classic Import tool' . PHP_EOL;

if($argc < 5) {
    echo 'Missing parameters, usage: ' . basename(__FILE__) . ' dbuser dbpassword sourcedatabase targetdatabase' . PHP_EOL;
    echo 'Env: DUMP_TARGET_SQL to display the queries to the target database';
    exit(1);
}
if(file_exists(TARGET_CLASSIC_IMAGES_STORAGE)) {
    echo 'FATAL: please, remove the directory ' . TARGET_CLASSIC_IMAGES_STORAGE;
    exit(1);
} else {
    mkdir(TARGET_CLASSIC_IMAGES_STORAGE);
}

if(!getenv('U1_TIME_SPECIAL_MEDIA_DIR')) {
    echo 'FATAL, path to time special media dir must be specified, the directory should contain all media images with the forma 1.img, 2.img, etc, that represents the u1 format';
    exit(1);
} else {
    define('TIME_SPECIALS_CLASIC_MEDIA_FOLDER', getenv('U1_TIME_SPECIAL_MEDIA_DIR'));
}

list($_, $user, $password, $sourceDb, $targetDb) = $argv;
class MysqliProxy extends mysqli {
    private $queries = [];
    
    public function query($sql, $resultMode = MYSQLI_STORE_RESULT) {
        $this->queries[] = $sql;
        return parent::query($sql);
    }
    
    /**
     * @return string[] Array of SQL queries
     */
    public function getQueries(): array {
        return $this->queries;
    }
}

$sourceDbConnection = @new MysqliProxy('127.0.0.1', $user, $password, $sourceDb);
$targetDbConnection = @new MysqliProxy('127.0.0.1', $user, $password, $targetDb);
$sourceDbConnection->set_charset('utf8');
$targetDbConnection->set_charset('utf8');

function escapeString(MysqliProxy $connection, string $input): string {
        return $connection->real_escape_string(html_entity_decode($input, ENT_QUOTES, 'UTF-8'));
}

class ImageObject {
    public $classicImageUrl;
    public $image;
    
    public function __construct(string $classicImageUrl) {
        $this->classicImageUrl = $classicImageUrl;
    }
    
    /**
     * Downloads the image, to a folder, and creates an uniqid for it
     *
     * @return string the uniqid image identifier with its extension
     * @author Kevin Guanche Darias
    */
    public function download(): string {
        $url = $this->normalizeUrl($this->classicImageUrl);
        $fileData = file_get_contents($url);
        if(!is_dir(TARGET_CLASSIC_IMAGES_STORAGE)) {
            mkdir(TARGET_CLASSIC_IMAGES_STORAGE);
        }
        $filename = $this->normalizeFilename($this->findFilename($url));
        $this->image = uniqid() . $this->findExtension($filename);
        file_put_contents(TARGET_CLASSIC_IMAGES_STORAGE . DIRECTORY_SEPARATOR. $this->image, $fileData);
        return $this->image;
    }
    
    private function normalizeUrl(string $url): string {
        if(strpos($url, '//') === 0) {
            return 'https:' . $url;
        } else if(strpos($url, 'http') === 0 || strpos($url, 'https') === 0) {
            return $url;
        } else {
            throw new Exception('Malformed URL!' . $url);
        }
    }
    
    private function findFilename(string $url): string {
        return substr($url, strrpos($url, '/') + 1);
    }
    
    private function normalizeFilename(string $filename): string {
        if(strrpos($filename, '.') !== false) {
            return $filename; 
        } else {
            return $filename . DEFAULT_EXTENSION;
        }
    }
    
    private function findExtension(string $filename): string {
        return substr($filename, strrpos($filename, '.'));
    }
}

class NgObjectRelation {
    public $id;
    public $objectDescription;
    public $refId;
    
    public function __construct(string $objectDescription, int $refId) {
        if(!in_array($objectDescription, ['UPGRADE', 'UNIT', 'TIME_SPECIAL'])) {
            throw new Exception('Object relation with object type ' . $objectDescription . ' is not supported');
        }
        $this->objectDescription = $objectDescription;
        $this->refId = $refId;
    }
    
    public function save(mysqli $connection) {
        $connection->query("INSERT INTO object_relations ( object_description, reference_id) VALUES ( '$this->objectDescription', $this->refId)");
        $this->id = $connection->insert_id;
    }
}

class CommonObject {
    public $classicId;
    public $ngId;
    public $name;
    public $image;
    public $description;
    public $order;
}

class CommonUsableObject extends CommonObject {
    public $points;
    public $time;
    public $pr;
    public $sr;
    public $type;
    public $ngObjectRelation;
}

class WithImprovementsObject extends CommonUsableObject{
    public $improvementId;
    public $moreSoldiersProduction = 0;
    public $morePrProduction = 0;
    public $moreSrProduction = 0;
    public $moreEnergyProducction = 0;
    public $moreCharge = 0;
    public $moreMissions = 0;
    public $moreAttackTroops = 0;
    public $moreShieldTroops = 0;
    public $moreHealthTroops = 0;
    public $moreAttackShips = 0;
    public $moreShieldShips = 0;
    public $moreHealthShips = 0;
    public $moreAttackDefenses = 0;
    public $moreShieldDefendes = 0;
    public $moreHealthDefendes = 0;
    public $moreResearchSpeed = 0;
    public $moreUnitBuildSpeed = 0;
}

class Upgrade extends WithImprovementsObject {
    public $lvlEffect = 0;
    
    public static function findUpgradeInstanceByClassicId(array $upgrades, int $classicId) {
        foreach($upgrades as $upgrade) {
            if($upgrade->classicId === $classicId)  {
                return $upgrade;
            }
        }
    }
}

class RequiredUpgrade {
    public $level;
    public $upgradeInstance;
    
    public function __construct(int $level, Upgrade $upgradeInstance) {
        $this->level = $level;
        $this->upgradeInstance = $upgradeInstance;
    }
}

abstract class WithRequirementsObject extends WithImprovementsObject {
    /** @var RequiredUpgrade[] */
    public $requiredUpgrades = [];

    /**
     * Abstract method that should invoke doDefineRequiredUpgrade() with the correct type and tableSufix
     * 
     * @param Upgrade[] $upgrades An array of instances of Upgrade, (already populated)
     * @param mysqli $connection Input connection
     * @author Kevin Guanche Darias
    */
    public abstract function addRequiredUpgrade(array $upgrades, mysqli $connection);

    /**
     * Defines the required upgrades of the target entity that has requirements
     * 
     * @param Upgrade[] $upgrades An array of upgrades (already populated)
     * @param string $tableSuffix The suffix of the target table
     * @param string $type The type (used to find id column name)
     * @param mysqli $source The U1 classic DB
     * @author Kevin Guanche Darias
     */
    protected function doDefineRequiredUpgrade(array $upgrades, string $tableSuffix, string $type, mysqli $source) {
        $result = $source->query("SELECT * FROM `Requisitos$tableSuffix` WHERE `cd$type`=$this->classicId AND `Nivel` > 0");
        while($row = $result->fetch_assoc()) {
            $upgrade = Upgrade::findUpgradeInstanceByClassicId($upgrades, $row["mcd"]);
            if($upgrade) {
                $this->requiredUpgrades[] = new RequiredUpgrade(+$row['Nivel'], $upgrade);
            }
        }
    }
}

class Unit extends WithRequirementsObject {
    public $isUnique = false;
    public $energy = 0;
    public $attack = 0;
    public $health = 0;
    public $shield = 0;
    public $charge = 0;
    
    public function addRequiredUpgrade(array $upgrades, mysqli $connection) {
        $cdType = $this->type === 'Tropas'
            ? 'Unidades'
            : $this->type;
        $this->doDefineRequiredUpgrade($upgrades, $this->type, $cdType,$connection);
    }
}

class Faction extends CommonObject {
    public $hidden;
    public $prName;
    public $prImage;
    public $srName;
    public $srImage;
    public $energyName;
    public $energyImage;
    public $initialPr;
    public $initialSr;
    public $initialEnergy;
    public $prProduction;
    public $srProduction;
    public $maxPlanets;
    
    /** @var Upgrade[] */
    public $upgrades = [];

    /** @var Unit[] */
    public $units = [];

    /** @var TimeSpecial[] */
    public $timeSpecials = [];
}

class TimeSpecial extends WithRequirementsObject {
    /** @var int */
    public $duration;

    /** @var int */
    public $rechargeTime;

    /** @var ImageStore */
    public $image;

    public function addRequiredUpgrade(array $upgrades, \mysqli $connection) {
        $this->doDefineRequiredUpgrade($upgrades, 'especialesderaza', 'Especial', $connection);
    }
}

class ImageStore {
    public $extension;
    public $checksum;
    public $filename;
    public $displayName;
    public $description;
    public $localPath;

    /**
     * Creates an imageStore representation from a u1 "imagenes" table
     * 
     * @param int $id
     * @param mysqli $source The u1 database
     * @author Kevin Guanche Darias
     */
    public static function createFromImageId(int $id, mysqli $source): ImageStore {
        $instance = new self;
        $instance->description = 'Media imported from classic u1';
        $instance->localPath = TIME_SPECIALS_CLASIC_MEDIA_FOLDER . DIRECTORY_SEPARATOR . "$id.img";
        if(!file_exists($instance->localPath)) {
            echo "Unwilling to continue as ImageStore with id $id doesn't exists in the filesystem, {$instance->localPath}";
            exit(11);
        }
        $result = $source->query("SELECT `Formato` FROM `imagenes` WHERE `cdImagen`= $id");
        if($result->num_rows) {
            $instance->extension = '.' . self::findExtension($result->fetch_object()->Formato);
            $instance->displayName = 'u1_classic_media_id' . $id . $instance->extension;
        } else {
            echo "\e[33mWarning: image with id $id doesn't exists\e[39m";
        }
        return $instance;
    }

    private static function findExtension(string $mime) {
        switch($mime) {
            case 'image/jpeg':
                $retVal = 'jpg';
                break;
            case 'image/png':
                $retVal = 'png';
                break;
            case 'image/gif':
                $retVal = 'gif';
                break;
        }
        return $retVal;
    }

    private function __construct() {

    }

    /**
     * Saves to the target owge database, and to the target filesystem
     * 
     * @param mysqli $target The OWGE database to save to
     * @return int The saved id
     * @author Kevin Guanche Darias
     */
    public function saveToDbAndDisk(mysqli $target): int {
        $this->checksum = md5_file($this->localPath);
        $this->filename = $this->checksum . '.' . $this->extension;
        copy($this->localPath, TARGET_CLASSIC_IMAGES_STORAGE . DIRECTORY_SEPARATOR . $this->filename);
        $target->query("REPLACE INTO `images_store` (checksum, filename, display_name, description)VALUES('$this->checksum','$this->filename','$this->displayName','$this->description')");
        return $target->insert_id;
    }
}

/**
 * Extracts the information from OWGE Classic
 *
 * @author Kevin Guanche Darias
*/
class ClassicExtractor {
    private $source;

    /** @var Faction[] */
    private $factionsArray = [];
    
    public function __construct(mysqli $source) {
        $this->source = $source;
        $this->createFactions();
    }
    
    public function getFactionsArray() {
        return $this->factionsArray;    
    }
    
    private function createFactions() {
        $result = $this->source->query("SELECT * FROM razas");
        $i = 1;
        while($row = $result->fetch_object()) {
            $currentFaction = new Faction();
            $this->doCommonFill($currentFaction, $row);
            $currentFaction->order = $i;
            $currentFaction->hidden = $row->Oculto;
            $currentFaction->prName = $row->NRecursoPrimario;
            $currentFaction->prImage = new ImageObject($row->ImagenRP);
            $currentFaction->srName = $row->NRecursoSecundario;
            $currentFaction->srImage = new ImageObject($row->ImagenRS);
            $currentFaction->energyName = FACTION_ENERGY_NAME;
            $currentFaction->energyImage = FACTION_ENERGY_ICON;
            $currentFaction->initialPr = $row->RecursoPrimario;
            $currentFaction->initialSr = $row->RecursoSecundario;
            $currentFaction->initialEnergy =  FACTION_INITIAL_ENERGY;
            $currentFaction->prProduction = $row->PRecursoPrimario / 3600;
            $currentFaction->srProduction = $row->PRecursoSecundario / 3600;
            $currentFaction->maxPlanets = FACTION_MAX_PLANETS;
            $currentFaction->upgrades = $this->fetchFactionUpgrades($currentFaction);
            $currentFaction->units = $this->fetchFactionUnits($currentFaction);
            $currentFaction->timeSpecials = $this->fetchFactionTimeSpecials($currentFaction);
            $i++;
            $this->factionsArray[] = $currentFaction;
        }
    }
    
    private function fetchFactionUpgrades(Faction $faction) {
        $classicId = $faction->classicId;
        $upgrades = [];
        $result = $this->source->query("SELECT * FROM mejoras WHERE rcd=$classicId");
        $i = 1;
        while($row = $result->fetch_object()) {
            $currentUpgrade = new Upgrade();
            $currentUpgrade->order = $i;
            $this->doCommonFill($currentUpgrade, $row);
            $this->doCommonUsableObjectFill($currentUpgrade, $row);
            $currentUpgrade->lvlEffect = $row->EfectoNivel;
            $currentUpgrade->type = $row->Tipo;
            $this->handleImprovements($row, $currentUpgrade);
            $i++;
            $upgrades[] = $currentUpgrade;
        }
        return $upgrades;
    }
    
    private function fetchFactionUnits(Faction $faction) {
        $classicId = $faction->classicId;
        return array_merge(
            [],
            $this->iterateUnitRows($this->source->query("SELECT u.*, hu.* FROM unidades u LEFT JOIN heroesunidades hu ON hu.cd_heroe = u.cdHeroe WHERE rcd=$classicId"), $faction, TROOPS_TYPE_ID),
            $this->iterateUnitRows($this->source->query("SELECT n.*, hn.* FROM naves n LEFT JOIN heroesnaves hn ON hn.cd_heroe = n.cdHeroe WHERE rcd=$classicId"), $faction, SHIPS_TYPE_ID),
            $this->iterateUnitRows($this->source->query("SELECT d.*, hd.* FROM defensas d LEFT JOIN heroesdefensas hd ON hd.cd_heroe = d.cdHeroe WHERE rcd=$classicId"), $faction, DEFENSES_TYPE_ID)
        );
        
    }

    private function fetchFactionTimeSpecials(Faction $faction): array {
        $classicId = $faction->classicId;
        $retVal = [];
        $result = $this->source->query("SELECT * FROM `especialesderaza` WHERE rcd = $classicId");
        while($row = $result->fetch_object()) {
            $timeSpecial = new TimeSpecial();
            $this->doCommonFill($timeSpecial, $row);
            $timeSpecial->duration = $row->Duracion;
            $timeSpecial->rechargeTime = $row->Recarga;
            $this->handleImprovements((object)unserialize($row->Atributos), $timeSpecial);
            $timeSpecial->image = ImageStore::createFromImageId($row->cdImagen, $this->source);
            $timeSpecial->addRequiredUpgrade($faction->upgrades, $this->source);
            $retVal[] = $timeSpecial;
        }
        return $retVal;
    }
    
    private function iterateUnitRows(mysqli_result $result, Faction $faction, int $type) {
        $units = [];
        $i = 1;
        while($row = $result->fetch_object()) {    
            $currentUnit = new Unit();
            $this->doCommonFill($currentUnit, $row);
            $this->doCommonUsableObjectFill($currentUnit, $row);
            $currentUnit->energy = $row->Energia;
            switch($type) {
                case TROOPS_TYPE_ID:
                    $indexIndicator = 'u';
                    $currentUnit->type = 'Tropas';
                    break;
                case SHIPS_TYPE_ID:
                    $indexIndicator = 'n';
                    $currentUnit->type = 'Naves';
                    break;
                case DEFENSES_TYPE_ID:
                    $indexIndicator = 'd';
                    $currentUnit->type = 'Defensas';
                    break;
                default:
                    throw new Exception('Bad parameter passed to type, value ' . $type);
            }
            $currentUnit->order = $i + $type;
            $this->fillUnitAttributes($currentUnit, $row, $indexIndicator);
            if($row->cdHeroe) {
                $currentUnit->isUnique = true;
                $this->handleImprovements($row, $currentUnit);
            }
            $currentUnit->addRequiredUpgrade($faction->upgrades, $this->source);
            $units[] = $currentUnit;
            $i++;
        }
        
        return $units;
    }
    private function fillUnitAttributes(Unit $unit, $row, string $indexIndicator) {
            $unit->attack = @$row->{"a$indexIndicator" . 'v'} ?? 0;
            $unit->health = @$row->{"r$indexIndicator" . 'v'} ?? 0;
            $unit->shield = @$row->{"e$indexIndicator" . 'v'} ?? 0;
            $unit->charge = @$row->{"c$indexIndicator" . 'v'} ?? 0;
    }
    
    private function doCommonFill(CommonObject $target, $row ) {
        $target->classicId = +$row->cd;
        $target->name = $row->Nombre;
        if(isset($row->Imagen)) {
            $target->image = new ImageObject($row->Imagen);
        }
        $target->description = $row->Descripcion;
    }
    
    private function doCommonUsableObjectFill(CommonUsableObject $target, $row) {
        $target->points = $row->Puntos;
        $target->time = $row->Tiempo;
        $target->pr = $row->RecursoPrimario;
        $target->sr = $row->RecursoSecundario;
    }
    
    private function handleImprovements($row, WithImprovementsObject $objectWithImprovements) {
        if(@$row->mpdsb) {
            $objectWithImprovements->moreSoldiersProduction = $row->mpdsp;
        }
        if(@$row->mpdrpb) {
            $objectWithImprovements->morePrProduction = $row->mpdrpp;
        }
        if(@$row->mpdrsb) {
            $objectWithImprovements->moreSrProduction = $row->mpdrsp;
        }
        if(@$row->mpdeb) {
            $objectWithImprovements->moreEnergyProducction = $row->mpdep;
        }
        if(@$row->mccb) {
            $objectWithImprovements->moreCharge = $row->mccp;
        }
        if(@$row->mmb) {
            $objectWithImprovements->moreMissions = $row->mmp;
        }
        if(@$row->matb) {
            $objectWithImprovements->moreAttackTroops = $row->matp;
        }
        if(@$row->metb) {
            $objectWithImprovements->moreShieldTroops = $row->metp;
        }
        if(@$row->mrtb) {
            $objectWithImprovements->moreHealthTroops = $row->mrtp;
        }
        if(@$row->manb) {
            $objectWithImprovements->moreAttackShips = $row->manp;
        }
        if(@$row->menb) {
            $objectWithImprovements->moreShieldShips = $row->menp;
        }
        if(@$row->mrnb) {
            $objectWithImprovements->moreHealthShips = $row->mrnp;
        }
        if(@$row->madb) {
            $objectWithImprovements->moreAttackDefenses = $row->madp;
        }
        if(@$row->medb) {
            $objectWithImprovements->moreShieldDefendes = $row->medp;
        }
        if(@$row->mrdp) {
            $objectWithImprovements->moreHealthDefendes = $row->mrdp;
        }
        if(@$row->mvib) {
            $objectWithImprovements->moreResearchSpeed = $row->mvip;
        }
        if(@$row->mvcb) {
            $objectWithImprovements->moreUnitBuildSpeed = $row->mvcp;
        }
    }
}

class ImportHandler {
    const COMMON_FIELDS = 'name, points, image, description, time, primary_resource, secondary_resource';
    private $connection;
    private $beenRaceRequirementId;
    private $haveUpgradeLevelRequirementId;
    
    public function __construct(mysqli $connection) {
        $this->connection = $connection;
        $result = $this->connection->query('SELECT id FROM requirements WHERE code = \'' . NG_BEEN_RACE_REQUIREMENT_CODE . '\' LIMIT 1;')->fetch_object();
        if(!$result) {
            throw new Exception('Invalid target database, no requirement of type ' . NG_BEEN_RACE_REQUIREMENT_CODE . ' exists');
        }
        $this->beenRaceRequirementId = $result->id;
        $result = $this->connection->query('SELECT id FROM requirements WHERE code = \'' . NG_HAVE_UPGRADE_LEVEL_REQUIREMENT_CODE . '\' LIMIT 1;')->fetch_object();
        if(!$result) {
            throw new Exception('Invalid target database, no requirement of type ' . NG_HAVE_UPGRADE_LEVEL_REQUIREMENT_CODE . ' exists');
        }
        $this->haveUpgradeLevelRequirementId = $result->id;
    }
    
    public function doImport(array $factions) {
        $this->connection->query('START TRANSACTION');
        try {
            foreach($factions as $faction) {
                echo "Importing faction $faction->name with id $faction->classicId!" . PHP_EOL;
                $this->importFaction($faction);
            }
            $this->connection->query('COMMIT');
        }catch(Exception $e) {
            $this->connection->query('ROLLBACK');
            throw $e;
        }
    }
    
    private function importFaction(Faction $faction) {
        $name = escapeString($this->connection, $faction->name);
        $description = escapeString($this->connection, $faction->description);
        $prName = escapeString($this->connection, $faction->prName);
        $srName = escapeString($this->connection, $faction->srName);
        $energyName = escapeString($this->connection, $faction->energyName);
        $image = $faction->image->download();
        $prImage = $faction->prImage->download();
        $srImage = $faction->srImage->download();
        copy( DYNAMIC_EXTRA_SOURCE . DIRECTORY_SEPARATOR .  FACTION_ENERGY_ICON, TARGET_CLASSIC_IMAGES_STORAGE . DIRECTORY_SEPARATOR);
        $this->connection->query(
            "INSERT INTO factions " .
            "(hidden, name, image, description, primary_resource_name, primary_resource_image, secondary_resource_name, secondary_resource_image, energy_name, energy_image, initial_primary_resource, initial_secondary_resource, initial_energy, primary_resource_production, secondary_resource_production, max_planets, cloned_improvements) VALUES"
            . '(\''. implode('\',\'', [
                $faction->hidden, $name, $image, $description, $prName, $prImage, $srName, $srImage, $energyName, $faction->energyImage, $faction->initialPr, $faction->initialSr, $faction->initialEnergy, $faction->prProduction, $faction->srProduction, $faction->maxPlanets, 0
            ]) . '\')'
        );
        $faction->ngId = $this->connection->insert_id;
        foreach($faction->upgrades as $upgrade) {
            $this->importFactionUpgrade($faction, $upgrade);
        }
        foreach($faction->units as $unit) {
            $this->importFactionUnit($faction, $unit);
        }
        foreach($faction->timeSpecials as $timeSpecial) {
            $this->importFactionTimeSpecial($faction, $timeSpecial);
        }
    }
    
    private function importFactionUpgrade(Faction $ownerFaction, Upgrade $upgrade) {
        $this->insertUpgrade($upgrade);
        $this->importImprovements($upgrade);
        $this->connection->query("UPDATE upgrades SET improvement_id = $upgrade->improvementId WHERE id = $upgrade->ngId");
        $this->importUnitTypeImprovements($upgrade);
        $upgrade->ngObjectRelation = new NgObjectRelation('UPGRADE', $upgrade->ngId);
        $upgrade->ngObjectRelation->save($this->connection);
        $this->connection->query(
            'INSERT INTO requirements_information ' .
            '(relation_id, requirement_id, second_value) VALUES ' .
            '(' . $upgrade->ngObjectRelation->id . ", $this->beenRaceRequirementId, $ownerFaction->ngId)"
        );
    }
    
    private function importFactionTimeSpecial(Faction $ownerFaction, TimeSpecial $timeSpecial) {
        $this->insertTimeSpecial($timeSpecial);
        $this->importImprovements($timeSpecial);
        $this->connection->query("UPDATE time_specials SET improvement_id = $timeSpecial->improvementId WHERE id = $timeSpecial->ngId");
        $this->importUnitTypeImprovements($timeSpecial);
        $timeSpecial->ngObjectRelation = new NgObjectRelation('TIME_SPECIAL', $timeSpecial->ngId);
        $timeSpecial->ngObjectRelation->save($this->connection);
        $this->connection->query(
            'INSERT INTO requirements_information ' .
            '(relation_id, requirement_id, second_value) VALUES ' .
            '(' . $timeSpecial->ngObjectRelation->id . ", $this->beenRaceRequirementId, $ownerFaction->ngId)"
        );
        $this->importRequirements($timeSpecial);

    }

    private function importImprovements(WithImprovementsObject $withImprovementsObject) {
        $this->connection->query(
            'INSERT INTO improvements ' .
            '( `more_primary_resource_production`, `more_secondary_resource_production`, `more_energy_production`, `more_charge_capacity`, `more_missions_value`, `more_upgrade_research_speed`, `more_unit_build_speed`) VALUES ' .
            "($withImprovementsObject->morePrProduction, $withImprovementsObject->moreSrProduction, $withImprovementsObject->moreEnergyProducction, $withImprovementsObject->moreCharge, $withImprovementsObject->moreMissions, $withImprovementsObject->moreResearchSpeed, $withImprovementsObject->moreUnitBuildSpeed)"
        );
        $withImprovementsObject->improvementId = $this->connection->insert_id;
        if($withImprovementsObject->moreSoldiersProduction) {
            $typeId = $this->findTypeId('unit_types', NG_SOLDIERS_UNIT_TYPE_NAME);
            $this->connection->query(
                'INSERT INTO improvements_unit_types ' .
                '(improvement_id, type, unit_type_id, value)VALUES' .
                "($withImprovementsObject->improvementId, 'AMOUNT', $typeId, $withImprovementsObject->moreSoldiersProduction)"
            );
        }
    }
    
    private function insertUpgrade(Upgrade $upgrade) {
        $name = escapeString($this->connection, $upgrade->name);
        $description = escapeString($this->connection, $upgrade->description);
        $image = $upgrade->image->download();
        $ngType = $this->findTypeId('upgrade_types', $upgrade->type);
        try {
            $this->connection->query(
                'INSERT INTO upgrades ' .
                '(' . self::COMMON_FIELDS . ', type, level_effect, cloned_improvements) VALUES ' .
                "('$name',$upgrade->points, '$image','$description', $upgrade->time, $upgrade->pr, $upgrade->sr, $ngType, $upgrade->lvlEffect, 0)"
            );
            $upgrade->ngId = $this->connection->insert_id;
        } catch (mysqli_sql_exception $e) {
            if(strpos($e->getMessage(), 'Duplicate entry') !== false) {
                $upgrade->name .= ' DUPLICATED';
                $this->insertUpgrade($upgrade);
            } else {
                throw $e;
            }
        }
    }

    private function insertTimeSpecial(TimeSpecial $timeSpecial) {
        $name = escapeString($this->connection, $timeSpecial->name);
        $description = escapeString($this->connection, $timeSpecial->description);
        $imageId = $timeSpecial->image->saveToDbAndDisk($this->connection);
        try {
            $this->connection->query(
                'INSERT INTO time_specials ' .
                '(name, description, image_id, duration, recharge_time, cloned_improvements) VALUES ' . 
                "('$name','$description','$imageId',$timeSpecial->duration, $timeSpecial->rechargeTime, 0)"
            );
            $timeSpecial->ngId = $this->connection->insert_id;
        } catch(mysqli_sql_exception $e) {
            if(strpos($e->getMessage(), 'Duplicate entry') !== false) {
                $timeSpecial->name .= ' DUPLICATED';
                $this->insertTimeSpecial($timeSpecial);
            } else {
                throw $e;
            }
        }
    }

    private function importUnitTypeImprovements(WithImprovementsObject $withImprovementsObject) {
        $indexMap = [
            [
                'properties' => 'Troops',
                'unit_type_id' => $this->findTypeId('unit_types', 'Tropas')
            ],
            [
                'properties' => 'Ships',
                'unit_type_id' => $this->findTypeId('unit_types', 'Naves')
            ],
            [
                'properties' => 'Defenses',
                'unit_type_id' => $this->findTypeId('unit_types', 'Defensas')
            ],
        ];
        foreach($indexMap as $currentTarget) {
            $attack = "moreAttack$currentTarget[properties]";
            $defense = "moreHealth$currentTarget[properties]";
            $shield = "moreShield$currentTarget[properties]";
            if(isset($withImprovementsObject->{$attack})) {
                $this->insertUnitTypeImprovement($withImprovementsObject, $currentTarget, 'ATTACK', $withImprovementsObject->{$attack});
            }
            if(isset($withImprovementsObject->{$defense})) {
                $this->insertUnitTypeImprovement($withImprovementsObject, $currentTarget, 'DEFENSE', $withImprovementsObject->{$defense});
            }
            if(isset($withImprovementsObject->{$shield})) {
                $this->insertUnitTypeImprovement($withImprovementsObject, $currentTarget, 'SHIELD', $withImprovementsObject->{$shield});
            }
        }
    }
    
    private function insertUnitTypeImprovement(WithImprovementsObject $objectWithImprovements, array $target, string $dbType, int $value) {
        if(!in_array($dbType, [ 'ATTACK', 'DEFENSE', 'SHIELD'])) {
            throw new Exception('Bad unitType improvement, improvement type, specified ' . $dbType);
        }
        if($value) {
            $this->connection->query(
                'INSERT INTO improvements_unit_types ' .
                '( improvement_id, type, unit_type_id, value) VALUES ' .
                "($objectWithImprovements->improvementId, '$dbType', $target[unit_type_id], $value)"
            );
        }
    }
    
    private function importFactionUnit(Faction $ownerFaction, Unit $unit) {
        if($unit->isUnique) {
            $this->importImprovements($unit);
            $this->importUnitTypeImprovements($unit);
        } else {
            $this->connection->query('INSERT INTO improvements () VALUES ()');
            $unit->improvementId = $this->connection->insert_id;
        }
        $this->insertUnit($unit);
        $this->connection->query("UPDATE units SET improvement_id = $unit->improvementId WHERE id = $unit->ngId");
        $unit->ngObjectRelation = new NgObjectRelation('UNIT', $unit->ngId);
        $unit->ngObjectRelation->save($this->connection);
        $this->connection->query(
            'INSERT INTO requirements_information ' .
            '(relation_id, requirement_id, second_value) VALUES ' .
            '(' . $unit->ngObjectRelation->id . ", $this->beenRaceRequirementId, $ownerFaction->ngId)"
        );
        $this->importRequirements($unit);
    }
    
    private function importRequirements(WithRequirementsObject $withRequirementsObject): void {
        foreach($withRequirementsObject->requiredUpgrades as $requiredUpgrade) {
            $this->connection->query(
                'INSERT INTO requirements_information ' .
                '(relation_id, requirement_id, second_value, third_value) VALUES ' .
                '(' . $withRequirementsObject->ngObjectRelation->id . ", $this->haveUpgradeLevelRequirementId, " . $requiredUpgrade->upgradeInstance->ngId . ", $requiredUpgrade->level)"
            );   
        }
    }

    private function insertUnit(Unit $unit) {
        $name = escapeString($this->connection, $unit->name);
        $description = escapeString($this->connection, $unit->description);
        $image = $unit->image->download();
        $ngType = $this->findTypeId('unit_types', $unit->type);
        $unique = $unit->isUnique ? 1 : 0;
        try {
            $this->connection->query(
                'INSERT INTO units ' .
                '(' . self::COMMON_FIELDS . ', type, energy, attack, health, shield, charge, is_unique, improvement_id, cloned_improvements) VALUES ' .
                "('$name',$unit->points, '$image','$description', $unit->time, $unit->pr, $unit->sr, $ngType, $unit->energy, $unit->attack, $unit->health, $unit->shield, $unit->charge, $unique, $unit->improvementId, 0)"
            );
            $unit->ngId = $this->connection->insert_id;
        } catch (mysqli_sql_exception $e) {
            if(strpos($e->getMessage(), 'Duplicate entry') !== false) {
                $unit->name .= ' DUPLICATED';
                $this->insertUnit($unit);
            } else {
                throw $e;
            }
        }
    }
    
    private function findTypeId(string $ngTypeTable, string $classicType): string {
        $escapedType = escapeString($this->connection, $classicType);
        $id = $this->connection->query("SELECT id FROM $ngTypeTable WHERE LOWER(name) = LOWER('$escapedType') LIMIT 1")->fetch_object();
        if(!$id) {
            echo "Notice: $ngTypeTable of name $classicType doesn't exists, will create it, also if unit type has a count limit or an image, will have to be manually added, as u1 has it hardcoded in the source :/" . PHP_EOL;
            if($ngTypeTable === 'unit_types') {
                $limit = strtolower($classicType) === strtolower(NG_SOLDIERS_UNIT_TYPE_NAME) ? NG_SOLDIERS_LIMIT : 'NULL';
                $this->connection->query("INSERT INTO $ngTypeTable ( name, max_count ) VALUES ('$escapedType', $limit)");
            } else {
                $this->connection->query("INSERT INTO $ngTypeTable ( name ) VALUES ('$escapedType')");
            }
            return $this->connection->insert_id;
        } else {
            return $id->id;
        }
    }
}

/**
 * Handles MySQLI exceptions occured inside the <i>$actions</i> <br>
 * <b>Notice:</b> Will rethrow the exception
 * 
 * @param MysqliProxy $db The connection that is expected to have thrown the exception
 * @param callable $actions The actions that may throw an exception
 * 
 */
function handleSqlException(MysqliProxy $db, callable $actions): void {
    try {
        $actions();
    } catch (mysqli_sql_exception $e) {
        $queries = $db->getQueries();
        $lastQuery = $queries[count($queries) -1];
        echo "Failed to execute: $lastQuery" . PHP_EOL;
        throw $e;
    }
}

// START Program itself
$extractor;
handleSqlException($sourceDbConnection, function() use ($sourceDbConnection, &$extractor) {
    $extractor = new ClassicExtractor($sourceDbConnection);
});
handleSqlException($targetDbConnection, function() use($targetDbConnection, $extractor) {
    $importHandler = new ImportHandler($targetDbConnection);
    $importHandler->doImport($extractor->getFactionsArray());
});


if(getenv('DUMP_TARGET_SQL')) {
    echo implode(PHP_EOL, $targetDbConnection->getQueries());
}
?>