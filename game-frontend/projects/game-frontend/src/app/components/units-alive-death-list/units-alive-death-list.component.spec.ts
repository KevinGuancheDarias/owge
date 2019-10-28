import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UnitsAliveDeathListComponent } from './units-alive-death-list.component';

describe('UnitsAliveDeathListComponent', () => {
  let component: UnitsAliveDeathListComponent;
  let fixture: ComponentFixture<UnitsAliveDeathListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UnitsAliveDeathListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UnitsAliveDeathListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
