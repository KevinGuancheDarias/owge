import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CanDoMissionsCrudComponent } from './can-do-missions-crud.component';

describe('CanDoMissionsCrudComponent', () => {
  let component: CanDoMissionsCrudComponent;
  let fixture: ComponentFixture<CanDoMissionsCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CanDoMissionsCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CanDoMissionsCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
