import { HttpHeaders } from '@angular/common/http';

export const MEDIA_ROUTES = {
    'IMAGES_ROOT': '/dynamic/',
    'STATIC_IMAGES_ROOT': '/static/img/',
    UI_ICONS: '/static/img/ui_icons/',
    PLANET_IMAGES: '/static/img/planet/',
    'PLANET_RICHNESS_IMAGES': '/static/img/planet_richness/'
};

export const ROUTES = {
    LOGIN: '/login',
    UNIVERSE_SELECTION: '/universe-selection',
    SYNCHRONIZE_CREDENTIALS: 'synchronice-credentials',
    GAME_INDEX: '/home',
    UPGRADES: '/upgrades',
    UNITS: '/units',
    NAVIGATE: '/navigate',
    PLANET_LIST: '/planet-list',
    REPORTS: '/reports',
    ALLIANCE: '/alliance',
    RANKING: '/ranking',
    VERSION: '/version',
    SETTINGS: '/settings',
    SYSTEM_MESSAGES: '/system-messages',
    SPONSORS: '/sponsors'
};

export class Config {
    public static readonly URL_ENCODE_UTF8 = 'application/x-www-form-urlencoded; charset=UTF-8';
    public static readonly PLANET_RICHNESS_IMAGE_EXTENSION = '.png';
    public static readonly PLANET_NOT_EXPLORED_IMAGE = 'unexplored.jpg';
    public static accountServerUrl = 'undefined';
    public static accountLoginendpoint = 'undefined';

    public static genCommonFormUrlencoded(): HttpHeaders {
        let headers = new HttpHeaders();
        headers = headers.append('Content-Type', this.URL_ENCODE_UTF8);
        return headers;
    }
}
