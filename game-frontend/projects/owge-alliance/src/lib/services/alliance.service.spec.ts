import { TestBed, inject } from '@angular/core/testing';

import { AllianceService } from './alliance.service';

describe('AllianceService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AllianceService]
    });
  });

  it('should be created', inject([AllianceService], (service: AllianceService) => {
    expect(service).toBeTruthy();
  }));
});
