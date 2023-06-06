import { Pipe, PipeTransform } from '@angular/core';
import { MEDIA_ROUTES } from '../pojos/config.pojo';


/**
 * finds the path to a dynamic image
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 * @export
 */
@Pipe({
    name: 'dynamicImage'
})
export class DynamicImagePipe implements PipeTransform {
    public transform(image: string): string {
        return MEDIA_ROUTES.IMAGES_ROOT + image;
    }

}
