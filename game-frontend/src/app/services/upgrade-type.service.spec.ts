import { TestBed, inject } from '@angular/core/testing';

import { UpgradeTypeService } from './upgrade-type.service';

describe('UpgradeTypeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UpgradeTypeService]
    });
  });

  it('should be created', inject([UpgradeTypeService], (service: UpgradeTypeService) => {
    expect(service).toBeTruthy();
  }));
});
