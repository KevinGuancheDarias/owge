import { Injectable } from '@angular/core';

@Injectable()
export class SwipeService {
    public handleSwipe(element: HTMLElement, listener: (verticalTouchDiff: number, horizontalTouchDiff) => void): () => void {
        let touchStartX = 0;
        let touchStartY = 0;
        const touchStartHandler = (e: TouchEvent) => {
            touchStartX = e.changedTouches[0].screenX;
            touchStartY = e.changedTouches[0].screenY;
        };
        const touchEndHandler = (e: TouchEvent) => {
            listener(
                touchStartX - e.changedTouches[0].screenX,
                touchStartY - e.changedTouches[0].screenY
            );
        };
        element.addEventListener('touchstart', touchStartHandler);
        element.addEventListener('touchend', touchEndHandler);
        return () => {
            element.removeEventListener('touchstart', touchStartHandler);
            element.removeEventListener('touchend', touchEndHandler);
        };
    }

    public screenYSwipeRatio(touchedPixels: number): number {
        return touchedPixels / window.screen.availHeight;
    }
}
