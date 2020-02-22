import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourceRequirementsCrudComponent } from './resource-requirements-crud.component';

describe('ResourceRequirementsCrudComponent', () => {
  let component: ResourceRequirementsCrudComponent;
  let fixture: ComponentFixture<ResourceRequirementsCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ResourceRequirementsCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourceRequirementsCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
