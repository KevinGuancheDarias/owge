import {RequiringInit} from '../interfaces/requiring-init.interface';

export function servicesInitializer(services: RequiringInit[]): () => Promise<void> {
  return async () => {
    for(let service of services) {
      await service.init();
    }
  }
}
