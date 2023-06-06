import { Pipe, PipeTransform } from '@angular/core';

import { MEDIA_ROUTES } from '@owge/core';

/**
 * Finds the UI ICON for an input image string
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 * @export
 */
@Pipe({
    name: 'uiIcon'
})
export class UiIconPipe implements PipeTransform {
    public transform(inputImage: string): string {
        return MEDIA_ROUTES.UI_ICONS + inputImage;
    }
}
