import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UnitCrudComponent } from './unit-crud.component';

describe('UnitCrudComponent', () => {
  let component: UnitCrudComponent;
  let fixture: ComponentFixture<UnitCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UnitCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UnitCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
