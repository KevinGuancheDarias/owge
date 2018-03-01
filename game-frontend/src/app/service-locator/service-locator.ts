import { Injectable, Injector } from '@angular/core';

@Injectable()
export class ServiceLocator {
    static injector: Injector;

    constructor(private injector: Injector) {
        ServiceLocator.injector = this.injector;
    }
}
