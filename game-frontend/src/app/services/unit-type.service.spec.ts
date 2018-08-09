import { TestBed, inject } from '@angular/core/testing';

import { UnitTypeService } from './unit-type.service';

describe('UnitTypeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UnitTypeService]
    });
  });

  it('should be created', inject([UnitTypeService], (service: UnitTypeService) => {
    expect(service).toBeTruthy();
  }));
});
