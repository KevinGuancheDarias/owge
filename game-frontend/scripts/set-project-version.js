const JSDOM = require('jsdom').JSDOM;
const replaceInFile = require('replace-in-file');

const pomFiles = [
    '../business/pom.xml',
    '../game-rest/pom.xml'
];
const packageJsonFiles = [
    './package.json',
    './projects/owge-alliance/package.json',
    './projects/owge-core/package.json',
    './projects/owge-faction/package.json',
    './projects/owge-galaxy/package.json',
    './projects/owge-universe/package.json',
    './projects/owge-widgets/package.json'
];
if (process.argv.length >= 3) {
    const targetVersion = process.argv[2];
    pomFiles.forEach(async pomFile => {
        console.log(`Changing version to  file ${pomFile}`);
        const dom = await JSDOM.fromFile(pomFile);
        const versionEl = dom.window.document.querySelector('project > version');
        replaceInFile({
            files: pomFile,
            from: `<version>${versionEl.innerHTML}</version>`,
            to: `<version>${targetVersion}-SNAPSHOT</version>`
        });
        const owgeVersionEl = Array.from(dom.window.document.querySelectorAll('project > properties *')).find(el => el.tagName === 'owge.version');
        if(owgeVersionEl) {
            replaceInFile({
                files: pomFile,
                from: `<owge.version>${owgeVersionEl.innerHTML}</owge.version>`,
                to: `<owge.version>${targetVersion}-SNAPSHOT</owge.version>`
            })
        }
    });
    packageJsonFiles.forEach(async packageJson => {
        console.log(`Changing version to  file ${packageJson}`);
        const oldVersion = require(`.${packageJson}`).version;
        replaceInFile({
            files: packageJson,
            from: `"version": "${oldVersion}"`,
            to: `"version": "${targetVersion}"`
        });
    });
} else {
    console.error('Must specify version, string must not contain the "v" prefix');
}
