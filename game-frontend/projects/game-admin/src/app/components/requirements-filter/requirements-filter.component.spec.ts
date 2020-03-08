import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RequirementsFilterComponent } from './requirements-filter.component';

describe('RequirementsFilterComponent', () => {
  let component: RequirementsFilterComponent;
  let fixture: ComponentFixture<RequirementsFilterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RequirementsFilterComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RequirementsFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
