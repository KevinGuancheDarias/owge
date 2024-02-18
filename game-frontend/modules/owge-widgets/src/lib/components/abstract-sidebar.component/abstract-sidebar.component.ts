import { TranslateService } from '@ngx-translate/core';
import { MenuRoute } from '@owge/core';

export abstract class AbstractSidebarComponent {

  public constructor(private _internalTranslateService: TranslateService) { }

  /**
   * Creates a translatable <i>MenuRouteType</i> (Handles translation of the menu button)
   *
   * @since 0.8.0
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param textIndex The name of the translation in the file
   * @param pathOrAction
   * @param icon
   * @param isConnectionRequired
   */
  protected _createTranslatableMenuRoute(
    textIndex: string,
    pathOrAction: string | Function,
    icon: string,
    isConnectionRequired = false,
    cssClassesObject?: { [key: string]: boolean }
  ): MenuRoute {
    const retVal: MenuRoute = {
      text: 'Loading...',
      icon,
      isConnectionRequired
    };
    if (typeof pathOrAction === 'function') {
      retVal.action = pathOrAction;
    } else {
      retVal.path = pathOrAction;
    }
    this._internalTranslateService.get(textIndex).subscribe(value => retVal.text = value);
    retVal.cssClasses = cssClassesObject;
    return retVal;
  }
}
