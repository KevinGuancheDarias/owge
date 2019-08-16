import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ListRunningMissionsComponent } from './list-running-missions.component';

describe('ListRunningMissionsComponent', () => {
  let component: ListRunningMissionsComponent;
  let fixture: ComponentFixture<ListRunningMissionsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ListRunningMissionsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListRunningMissionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
