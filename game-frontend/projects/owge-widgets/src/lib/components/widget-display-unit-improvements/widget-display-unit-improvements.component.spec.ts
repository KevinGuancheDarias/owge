import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplayUnitImprovementsComponent } from './widget-display-unit-improvements.component';

describe('WidgetDisplayUnitImprovementsComponent', () => {
  let component: WidgetDisplayUnitImprovementsComponent;
  let fixture: ComponentFixture<WidgetDisplayUnitImprovementsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetDisplayUnitImprovementsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplayUnitImprovementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
