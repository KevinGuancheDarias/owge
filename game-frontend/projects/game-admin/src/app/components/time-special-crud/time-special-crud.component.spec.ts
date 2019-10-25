import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TimeSpecialCrudComponent } from './time-special-crud.component';

describe('TimeSpecialCrudComponent', () => {
  let component: TimeSpecialCrudComponent;
  let fixture: ComponentFixture<TimeSpecialCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TimeSpecialCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TimeSpecialCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
