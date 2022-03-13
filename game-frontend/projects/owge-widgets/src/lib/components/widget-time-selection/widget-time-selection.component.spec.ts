import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetTimeSelectionComponent } from './widget-time-selection.component';

describe('WidgetTimeSelectionComponent', () => {
  let component: WidgetTimeSelectionComponent;
  let fixture: ComponentFixture<WidgetTimeSelectionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WidgetTimeSelectionComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetTimeSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
