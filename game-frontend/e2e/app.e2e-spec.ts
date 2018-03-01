import { GameFrontendPage } from './app.po';

describe('game-frontend App', function() {
  let page: GameFrontendPage;

  beforeEach(() => {
    page = new GameFrontendPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
