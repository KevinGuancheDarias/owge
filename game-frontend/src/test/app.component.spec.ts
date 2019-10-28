import { testingConfig } from '../settings';
import { PlanetPojo } from './../app/shared-pojo/planet.pojo';
import { GameCommonTestHelper } from './../helpers/game-common-test.helper';
import { AppComponent } from './../app/app.component';
import { CommonComponentTestHelper } from './../helpers/common-component-test.helper';

describe('AppComponent', () => {
    const helper: CommonComponentTestHelper<AppComponent> = new CommonComponentTestHelper(AppComponent, testingConfig);
    const gameHelper: GameCommonTestHelper<CommonComponentTestHelper<AppComponent>> = new GameCommonTestHelper(helper);
    helper.testItCreates();

    helper.testAsync('Should keep sync isInGame attribute', () => {
        gameHelper.fakeLoginSessionServiceIsInGame(false);
        helper.component.ngOnInit();
        expect(helper.component.isInGame).toBeFalsy();

        gameHelper.fakeLoginSessionServiceIsInGame(true);
        helper.component.ngOnInit();
        expect(helper.component.isInGame).toBeTruthy();

    });

    helper.testAsync('Should keep sync selectedPlanet attribute', () => {
        gameHelper.fakeLoginSessionServiceFindSelectedPlanet(null);
        helper.component.ngOnInit();
        expect(helper.component.selectedPlanet).toBeNull();

        gameHelper.fakeLoginSessionServiceFindSelectedPlanet(new PlanetPojo());
        helper.component.ngOnInit();
        expect(helper.component.selectedPlanet).toEqual(jasmine.any(PlanetPojo));

    });

    helper.testAsync('Should have side-bard when is in game', () => {
        gameHelper.fakeLoginSessionServiceIsInGame(true);
        helper.component.isInGame = true;
        helper.startNgLifeCycle();
        helper.testHtmlElementPresent('owge-widgets-sidebar');

        gameHelper.fakeLoginSessionServiceIsInGame(false);
        helper.component.isInGame = false;
        helper.startNgLifeCycle();
        helper.testHtmlElementPresent('owge-widgets-sidebar', true);
    });
});
