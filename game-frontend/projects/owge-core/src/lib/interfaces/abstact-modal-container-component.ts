import { ModalComponent } from '../components/modal/modal.component';
import { ViewChild, Directive } from '@angular/core';
import { AbstractModalComponent } from './abstract-modal-component';


/**
 * Used to create components containing just one modal
 *
 * @since 0.7.0
 * @example MissionModalComponent
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
@Directive()
export abstract class AbstractModalContainerComponent extends AbstractModalComponent {
    @ViewChild('childModal', { static: true })
    public _childModal: ModalComponent;

    public show(): void {
        this._childModal.show();
    }

    public hide(): void {
        this._childModal.hide();
    }
}
