import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FastExplorationButtonComponent } from './fast-exploration-button.component';

describe('FastExplorationButtonComponent', () => {
  let component: FastExplorationButtonComponent;
  let fixture: ComponentFixture<FastExplorationButtonComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FastExplorationButtonComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FastExplorationButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
