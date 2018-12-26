import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PlanetSelectorComponent } from './planet-selector.component';

describe('PlanetSelectorComponent', () => {
  let component: PlanetSelectorComponent;
  let fixture: ComponentFixture<PlanetSelectorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PlanetSelectorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PlanetSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
