import { TranslateService } from '@ngx-translate/core';
import { MenuRoute } from '@owge/core';

export abstract class AbstractSidebarComponent {

    public constructor(private _internalTranslateService: TranslateService) { }

   /**
   * Creates a translatable <i>MenuRoute</i> (Handles translation of the menu button)
   *
   * @since 0.8.0
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param textIndex The name of the translation in the file
   * @param path
   * @param icon
   */
  protected _createTranslatableMenuRoute(textIndex: string, path: string, icon: string): MenuRoute {
    const retVal: MenuRoute =  {
      text: 'Loading...',
      path,
      icon
    };
    this._internalTranslateService.get(textIndex).subscribe(value => retVal.text = value);
    return retVal;
  }
}
