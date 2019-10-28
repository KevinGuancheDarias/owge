console.log('Compiling markdown file CHANGELOG.md');
const showdown = require('showdown');
const fs = require('fs');
const converter = new showdown.Converter();
const html = converter.makeHtml(fs.readFileSync('./CHANGELOG.md', 'UTF-8'));
const targetFile = './projects/game-frontend/src/assets/html/changelog.html';
if (fs.existsSync(targetFile)) {
    fs.unlinkSync(targetFile);
}
fs.writeFileSync(targetFile, html, 'UTF-8');