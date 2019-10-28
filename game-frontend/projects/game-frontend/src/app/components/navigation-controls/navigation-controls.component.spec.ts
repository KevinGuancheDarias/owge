import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NavigationControlsComponent } from './navigation-controls.component';

describe('NavigationControlsComponent', () => {
  let component: NavigationControlsComponent;
  let fixture: ComponentFixture<NavigationControlsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NavigationControlsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NavigationControlsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
