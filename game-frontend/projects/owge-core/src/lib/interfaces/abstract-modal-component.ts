import { Input } from '@angular/core';

/**
 * Represents a component that has a modal, and has input to that modal and methods to open/close the modal
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @export
 */
export abstract class AbstractModalComponent {
    @Input()
    public closeOnOverlayClick = true;

    @Input()
    public isOpenOnLoad = false;

    @Input()
    public hasCloseButton = false;

    public abstract show(): void;
    public abstract hide(): void;
}
