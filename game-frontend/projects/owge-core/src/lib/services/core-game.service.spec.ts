import { TestBed, inject } from '@angular/core/testing';

import { CoreGameService } from './core-game.service';

describe('CoreGameService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [CoreGameService]
    });
  });

  it('should be created', inject([CoreGameService], (service: CoreGameService) => {
    expect(service).toBeTruthy();
  }));
});
