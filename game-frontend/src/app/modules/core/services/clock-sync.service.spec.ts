import { TestBed, inject } from '@angular/core/testing';

import { ClockSyncService } from './clock-sync.service';

describe('ClockSyncService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ClockSyncService]
    });
  });

  it('should be created', inject([ClockSyncService], (service: ClockSyncService) => {
    expect(service).toBeTruthy();
  }));
});
