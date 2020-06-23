import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RequirementsModalComponent } from './requirements-modal.component';

describe('RequirementsModalComponent', () => {
  let component: RequirementsModalComponent;
  let fixture: ComponentFixture<RequirementsModalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RequirementsModalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RequirementsModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
