import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DisplayQuadrantComponent } from './display-quadrant.component';

describe('DisplayQuadrantComponent', () => {
  let component: DisplayQuadrantComponent;
  let fixture: ComponentFixture<DisplayQuadrantComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DisplayQuadrantComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DisplayQuadrantComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
