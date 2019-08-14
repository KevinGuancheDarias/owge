import { TestBed } from '@angular/core/testing';

import { OwgeFactionService } from './owge-faction.service';

describe('OwgeFactionService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: OwgeFactionService = TestBed.get(OwgeFactionService);
    expect(service).toBeTruthy();
  });
});
