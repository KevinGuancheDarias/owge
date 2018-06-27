import { TestBed, inject } from '@angular/core/testing';

import { SanitizeService } from './sanitize.service';

describe('SanitizeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SanitizeService]
    });
  });

  it('should be created', inject([SanitizeService], (service: SanitizeService) => {
    expect(service).toBeTruthy();
  }));
});
