import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ObjectRequirementGroupsCrudComponent } from './object-requirement-groups-crud.component';

describe('ObjectRequirementGroupsCrudComponent', () => {
  let component: ObjectRequirementGroupsCrudComponent;
  let fixture: ComponentFixture<ObjectRequirementGroupsCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ObjectRequirementGroupsCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectRequirementGroupsCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
