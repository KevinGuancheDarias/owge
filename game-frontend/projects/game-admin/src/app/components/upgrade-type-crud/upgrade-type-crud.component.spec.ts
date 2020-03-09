import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UpgradeTypeCrudComponent } from './upgrade-type-crud.component';

describe('UpgradeTypeCrudComponent', () => {
  let component: UpgradeTypeCrudComponent;
  let fixture: ComponentFixture<UpgradeTypeCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UpgradeTypeCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UpgradeTypeCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
