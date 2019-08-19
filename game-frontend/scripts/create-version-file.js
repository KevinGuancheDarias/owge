// Modern Angular doesn't like requiring the package,json when --aot compilation is enabled, so we have to find another way

const fs = require('fs');
const { version } = require('../package.json');

console.log(`Creating version file for version ${version}`);
fs.writeFileSync('projects/game-frontend/src/version.ts', `
    // Dinamically created file from npm run scripts
    export const version = '${version}';\n
`, 'utf-8');