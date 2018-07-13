#!/usr/bin/env php
<?php
define('TARGET_CLASSIC_IMAGES_STORAGE', 'sgt_classic_media');
define('FACTION_INITIAL_ENERGY', 4200);
define('FACTION_ENERGY_ICON','sgt_classic.png');
define('FACTION_ENERGY_NAME', 'Energy');
define('FACTION_MAX_PLANETS', 4);
define('TROOPS_TYPE_ID', 0);
define('SHIPS_TYPE_ID', 100);
define('DEFENSES_TYPE_ID', 1000);
define('CLASSIC_SGT_DB_DELIMITER', ';');
define('DEFAULT_EXTENSION', '.jpg');
define('NG_BEEN_RACE_REQUIREMENT_CODE', 'BEEN_RACE');
define('NG_HAVE_UPGRADE_LEVEL_REQUIREMENT_CODE', 'UPGRADE_LEVEL');

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

if($argc < 5) {
    echo 'Missing parameters, usage: ' . basename(__FILE__) . ' dbuser dbpassword sourcedatabase targetdatabase' . PHP_EOL;
    echo 'Env: DUMP_TARGET_SQL to display the queries to the target database';
    exit(1);
}
list($_, $user, $password, $sourceDb, $targetDb) = $argv;
class MysqliProxy extends mysqli {
    private $connection;
    private $queries = [];
    
    public function query($sql, $resultMode = MYSQLI_STORE_RESULT) {
        $this->queries[] = $sql;
        return parent::query($sql);
    }
    
    public function getQueries(): array {
        return $this->queries;
    }
}

$sourceDbConnection = @new MysqliProxy('127.0.0.1', $user, $password, $sourceDb);
$targetDbConnection = @new MysqliProxy('127.0.0.1', $user, $password, $targetDb);
$sourceDbConnection->set_charset('utf-8');
$targetDbConnection->set_charset('utf-8');

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
        if(!in_array($objectDescription, ['UPGRADE', 'UNIT'])) {
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
    public $improvementId;
}

class Upgrade extends CommonUsableObject {
    public $lvlEffect = 0;
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

class Unit extends CommonUsableObject {
    public $energy = 0;
    public $attack = 0;
    public $health = 0;
    public $shield = 0;
    public $charge = 0;
    public $requiredUpgrades = [];
    
    /**
     * @param array $upgrades An array of instances of Upgrade, (already populated)
     * @param string $upgradesIds String of delimited upgrade ids (as stored in classic SGT)
     * @param string $upgradeLevels String of delimited upgrade levels (as stored in classic SGT)
     * @author Kevin Guanche Darias
    */
    public function addRequiredUpgrade(array $upgrades, string $upgradesIds, string $upgradeLevels) {
        $ids = explode(CLASSIC_SGT_DB_DELIMITER, $upgradesIds);
        $levels = explode(CLASSIC_SGT_DB_DELIMITER, $upgradeLevels);
        foreach($ids as $key=>$value) {
            if(isset($levels[$key]) && $levels[$key]) {
                
                $upgrade = Upgrade::findUpgradeInstanceByClassicId($upgrades, +$value);
                if($upgrade) {
                    $this->requiredUpgrades[] = new RequiredUpgrade(+$levels[$key], $upgrade);
                }
            }
        }
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
    
    public $upgrades = [];
    public $units = [];
}

/**
 * Extracts the information from SGT Classic
 *
 * @author Kevin Guanche Darias
*/
class ClassicExtractor {
    private $source;
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
            if($row->mpdsb) {
                $currentUpgrade->moreSoldiersProduction = $row->mpdsp;
            }
            if($row->mpdrpb) {
                $currentUpgrade->morePrProduction = $row->mpdrpp;
            }
            if($row->mpdrsb) {
                $currentUpgrade->moreSrProduction = $row->mpdrsp;
            }
            if($row->mpdeb) {
                $currentUpgrade->moreEnergyProducction = $row->mpdep;
            }
            if($row->mccb) {
                $currentUpgrade->moreCharge = $row->mccp;
            }
            if($row->mmb) {
                $currentUpgrade->moreMissions = $row->mmp;
            }
            if($row->matb) {
                $currentUpgrade->moreAttackTroops = $row->matp;
            }
            if($row->metb) {
                $currentUpgrade->moreShieldTroops = $row->metp;
            }
            if($row->mrtb) {
                $currentUpgrade->moreHealthTroops = $row->mrtp;
            }
            if($row->manb) {
                $currentUpgrade->moreAttackShips = $row->manp;
            }
            if($row->menb) {
                $currentUpgrade->moreShieldShips = $row->menp;
            }
            if($row->mrnb) {
                $currentUpgrade->moreHealthShips = $row->mrnp;
            }
            if($row->madb) {
                $currentUpgrade->moreAttackDefenses = $row->madp;
            }
            if($row->medb) {
                $currentUpgrade->moreShieldDefendes = $row->medp;
            }
            if($row->mrdp) {
                $currentUpgrade->moreHealthDefendes = $row->mrdp;
            }
            $i++;
            $upgrades[] = $currentUpgrade;
        }
        return $upgrades;
    }
    
    private function fetchFactionUnits(Faction $faction) {
        $classicId = $faction->classicId;
        return array_merge(
            [],
            $this->iterateUnitRows($this->source->query("SELECT * FROM unidades WHERE rcd=$classicId"), $faction, TROOPS_TYPE_ID),
            $this->iterateUnitRows($this->source->query("SELECT * FROM naves WHERE rcd=$classicId"), $faction, SHIPS_TYPE_ID),
            $this->iterateUnitRows($this->source->query("SELECT * FROM defensas WHERE rcd=$classicId"), $faction, DEFENSES_TYPE_ID)
        );
        
    }
    
    private function iterateUnitRows(mysqli_result $result, Faction $faction, int $type) {
        $units = [];
        $i = 1;
        while($row = $result->fetch_object()) {    
            if($row->cdHeroe) {
                echo 'Ignoring unit with id ' . $row->cd . ' as it is an hero, and are currently NOT supported' . PHP_EOL;
            } else {
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
                $currentUnit->addRequiredUpgrade($faction->upgrades, $row->RequisitosMejorasmcd, $row->RequisitosMejoraslvl);
                $units[] = $currentUnit;
            }
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
        $target->image = new ImageObject($row->Imagen);
        $target->description = $row->Descripcion;
    }
    
    private function doCommonUsableObjectFill(CommonUsableObject $target, $row) {
        $target->points = $row->Puntos;
        $target->time = $row->Tiempo;
        $target->pr = $row->RecursoPrimario;
        $target->sr = $row->RecursoSecundario;
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
    }
    
    private function importFactionUpgrade(Faction $ownerFaction, Upgrade $upgrade) {
        $this->insertUpgrade($upgrade);
        $upgrade->ngId = $this->connection->insert_id;
        $this->connection->query(
            'INSERT INTO improvements ' .
            '( `more_soldiers_production`, `more_primary_resource_production`, `more_secondary_resource_production`, `more_energy_production`, `more_charge_capacity`, `more_missions_value`) VALUES ' .
            "($upgrade->moreSoldiersProduction, $upgrade->morePrProduction, $upgrade->moreSrProduction, $upgrade->moreEnergyProducction, $upgrade->moreCharge, $upgrade->moreMissions)"
        );
        $upgrade->improvementId = $this->connection->insert_id;
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
        } catch (mysqli_sql_exception $e) {
            if(strpos($e->getMessage(), 'Duplicate entry') !== false) {
                $upgrade->name .= ' DUPLICATED';
                $this->insertUpgrade($upgrade);
            } else {
                throw $e;
            }
        }
    }
    private function importUnitTypeImprovements(Upgrade $upgrade) {
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
            if(isset($upgrade->{$attack})) {
                $this->insertUnitTypeImprovement($upgrade, $currentTarget, 'ATTACK', $upgrade->{$attack});
            }
            if(isset($upgrade->{$defense})) {
                $this->insertUnitTypeImprovement($upgrade, $currentTarget, 'DEFENSE', $upgrade->{$defense});
            }
            if(isset($upgrade->{$shield})) {
                $this->insertUnitTypeImprovement($upgrade, $currentTarget, 'SHIELD', $upgrade->{$shield});
            }
        }
    }
    
    private function insertUnitTypeImprovement(Upgrade $upgrade, array $target, string $dbType, int $value) {
        if(!in_array($dbType, [ 'ATTACK', 'DEFENSE', 'SHIELD'])) {
            throw new Exception('Bad unitType improvement, improvement type, specified ' . $dbType);
        }
        if($value) {
            $this->connection->query(
                'INSERT INTO improvements_unit_types ' .
                '( improvement_id, type, unit_type_id, value) VALUES ' .
                "($upgrade->improvementId, '$dbType', $target[unit_type_id], $value)"
            );
        }
    }
    
    private function importFactionUnit(Faction $ownerFaction, Unit $unit) {
        $this->connection->query('INSERT INTO improvements () VALUES ()');
        $unit->improvementId = $this->connection->insert_id;
        $this->insertUnit($unit);
        $unit->ngId = $this->connection->insert_id;
        $this->connection->query("UPDATE units SET improvement_id = $unit->improvementId WHERE id = $unit->ngId");
        $unit->ngObjectRelation = new NgObjectRelation('UNIT', $unit->ngId);
        $unit->ngObjectRelation->save($this->connection);
        $this->connection->query(
            'INSERT INTO requirements_information ' .
            '(relation_id, requirement_id, second_value) VALUES ' .
            '(' . $unit->ngObjectRelation->id . ", $this->beenRaceRequirementId, $ownerFaction->ngId)"
        );
        foreach($unit->requiredUpgrades as $requiredUpgrade) {
            $this->connection->query(
                'INSERT INTO requirements_information ' .
                '(relation_id, requirement_id, second_value, third_value) VALUES ' .
                '(' . $unit->ngObjectRelation->id . ", $this->haveUpgradeLevelRequirementId, " . $requiredUpgrade->upgradeInstance->ngId . ", $requiredUpgrade->level)"
            );   
        }
    }
    
    private function insertUnit(Unit $unit) {
        $name = escapeString($this->connection, $unit->name);
        $description = escapeString($this->connection, $unit->description);
        $image = $unit->image->download();
        $ngType = $this->findTypeId('unit_types', $unit->type);
        try {
            $this->connection->query(
                'INSERT INTO units ' .
                '(' . self::COMMON_FIELDS . ', type, energy, attack, health, shield, charge, improvement_id, cloned_improvements) VALUES ' .
                "('$name',$unit->points, '$image','$description', $unit->time, $unit->pr, $unit->sr, $ngType, $unit->energy, $unit->attack, $unit->health, $unit->shield, $unit->charge, $unit->improvementId, 0)"
            );
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
            return 'NULL';
        } else {
            return $id->id;
        }
    }
}

// START Program itself
$extractor = new ClassicExtractor($sourceDbConnection);
$importHandler = new ImportHandler($targetDbConnection);
$importHandler->doImport($extractor->getFactionsArray());
if(getenv('DUMP_TARGET_SQL')) {
    echo implode(PHP_EOL, $targetDbConnection->getQueries());
}
?>