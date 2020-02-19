import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UnitTypeCrudComponent } from './unit-type-crud.component';

describe('UnitTypeCrudComponent', () => {
  let component: UnitTypeCrudComponent;
  let fixture: ComponentFixture<UnitTypeCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UnitTypeCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UnitTypeCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
