import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UnitRequirementsComponent } from './unit-requirements.component';

describe('UnitRequirementsComponent', () => {
  let component: UnitRequirementsComponent;
  let fixture: ComponentFixture<UnitRequirementsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UnitRequirementsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UnitRequirementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
