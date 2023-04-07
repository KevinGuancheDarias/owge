import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplaySuspicionComponent } from './display-suspicion.component';

describe('DisplaySuspicionComponent', () => {
  let component: DisplaySuspicionComponent;
  let fixture: ComponentFixture<DisplaySuspicionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DisplaySuspicionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplaySuspicionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
