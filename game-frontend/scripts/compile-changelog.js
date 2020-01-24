const JSDOM = require('jsdom').JSDOM;
console.log('Compiling markdown file CHANGELOG.md');
const showdown = require('showdown');
const fs = require('fs');
const converter = new showdown.Converter();
const html = converter.makeHtml(fs.readFileSync('./CHANGELOG.md', 'UTF-8'));
const targetFile = './projects/game-frontend/src/assets/html/changelog.html';
if (fs.existsSync(targetFile)) {
    fs.unlinkSync(targetFile);
}

/**
 * Removes the specified html tags,
 * 
 * @example removeHtmlTag(input, 'html','body')
 * 
 * @param {string} inputString
 * @param {string[]} tags
 * @retun {string}
 */
function removeHtmlTags(inputString, ...tags) {
    return tags.reduce((buffer, current) => buffer.replace(`<${current}>`, '').replace(`</${current}>`, ''), inputString);
}

(async () => {
    const dom = new JSDOM(html);
    const usedClasses = new Set();
    Array.from(dom.window.document.querySelectorAll('li')).forEach(currentNode => {
        const match = currentNode.innerHTML.match(/\[class=([a-zA-Z]+)\]/s);
        if (match && match.length === 2) {
            const [fullMatch, className] = match;
            currentNode.className = className;
            usedClasses.add(className);
            currentNode.innerHTML = currentNode.innerHTML.replace(fullMatch, '');
        }
    });
    const el = dom.window.document.createElement('div');
    el.setAttribute('comment', 'This div has the classes used in the changelog file');
    el.className = 'changelog-classes';
    el.style.display = 'none';
    Array.from(usedClasses).forEach(current => {
        const currentClassSpan = dom.window.document.createElement('span');
        currentClassSpan.innerHTML = current;
        el.appendChild(currentClassSpan);
    });
    dom.window.document.body.appendChild(el);

    const parsedResult = removeHtmlTags(dom.serialize(), 'html', 'head', 'body');
    fs.writeFileSync(targetFile, parsedResult, 'UTF-8');
})();
