import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TimeSpecialsComponent } from './time-specials.component';

describe('TimeSpecialsComponent', () => {
  let component: TimeSpecialsComponent;
  let fixture: ComponentFixture<TimeSpecialsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TimeSpecialsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimeSpecialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
