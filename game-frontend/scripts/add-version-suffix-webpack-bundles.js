const JSDOM = require('jsdom').JSDOM;
const fs = require('fs');
const targetFile = process.argv[2] ? process.argv[2] : 'dist/game-frontend/index.html';
const version = require('../package.json').version;
console.log(`Adding version suffix to all webpack bundles of target ${targetFile} file`);
(async () => {
    const dom = await JSDOM.fromFile(targetFile);
    dom.window.document.querySelectorAll('body script').forEach(script => {
        if (script.src.indexOf('?version=') === -1) {
            script.src = script.getAttribute('src') + `?version=${version}`;
        }
    });
    fs.writeFileSync(targetFile, dom.serialize());
})();