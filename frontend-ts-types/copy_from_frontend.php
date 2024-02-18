<?php
while ($line = fgets(STDIN)) {
    $line = trim($line);
    if(file_exists($line)) {
        $lastSlash = strrpos($line,"/");
        $filename = substr($line, $lastSlash + 1);
        preg_match('/\/modules\/owge-([a-z]*)\//', $line, $matches);
        $module = "types/$matches[1]";
        echo $filename  . " mod = $module ". PHP_EOL;
        if(!is_dir($module)) {
            mkdir($module,0, true);
        }
        $filenameWithoutExtension = substr($filename, 0, strrpos($filename,".")) . '.d.ts';
        copy($line, $module ."/". $filenameWithoutExtension);
        $typesDir = opendir("types");
        while (($currentTypesDir = readdir($typesDir)) !== false) {
            if($currentTypesDir == "."|| $currentTypesDir == "..") continue;
            $files = '';
            $moduleDir = opendir("types/$currentTypesDir");
            while (($currentTs = readdir($moduleDir)) !== false) {
                if($currentTs == "."|| $currentTs == "..") continue;
                $files .= "export * from './$currentTs';" . PHP_EOL;
            }
            closedir($moduleDir);
            file_put_contents("types/$currentTypesDir/index.d.ts", $files);
        }
        closedir($typesDir);

    } else {
        echo "\e[31mStrange error, file $line doesn't exists \e[39m" . PHP_EOL;
    }
}
