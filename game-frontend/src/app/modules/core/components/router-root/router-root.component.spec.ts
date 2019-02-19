import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RouterRootComponent } from './router-root.component';

describe('RouterRootComponent', () => {
  let component: RouterRootComponent;
  let fixture: ComponentFixture<RouterRootComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RouterRootComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RouterRootComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
