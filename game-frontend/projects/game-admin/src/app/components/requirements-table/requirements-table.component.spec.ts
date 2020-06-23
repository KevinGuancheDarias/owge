import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RequirementsTableComponent } from './requirements-table.component';

describe('RequirementsTableComponent', () => {
  let component: RequirementsTableComponent;
  let fixture: ComponentFixture<RequirementsTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RequirementsTableComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RequirementsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
