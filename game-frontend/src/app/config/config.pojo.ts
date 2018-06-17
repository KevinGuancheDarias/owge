import { Headers } from '@angular/http';

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
    GAME_INDEX: '/home',
    UPGRADES: '/upgrades',
    UNITS: '/units',
    NAVIGATE: '/navigate',
    REPORTS: '/reports'
};

export class Config {
    public static readonly ACCOUNT_SERVER_URL = 'http://localhost:8080/sgtjava-account/';
    public static readonly URL_ENCODE_UTF8 = 'application/x-www-form-urlencoded; charset=UTF-8';
    public static readonly PLANET_RICHNESS_IMAGE_EXTENSION = '.gif';
    public static readonly PLANET_NOT_EXPLORED_IMAGE = 'unexplored.jpg';

    public static genCommonFormUrlencoded(): Headers {
        const headers = new Headers();
        headers.append('Content-Type', this.URL_ENCODE_UTF8);
        return headers;
    }
}
