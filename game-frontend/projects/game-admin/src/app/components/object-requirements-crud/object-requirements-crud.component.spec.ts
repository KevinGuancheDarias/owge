import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ObjectRequirementsCrudComponent } from './object-requirements-crud.component';

describe('ObjectRequirementsCrudComponent', () => {
  let component: ObjectRequirementsCrudComponent;
  let fixture: ComponentFixture<ObjectRequirementsCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ObjectRequirementsCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectRequirementsCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
