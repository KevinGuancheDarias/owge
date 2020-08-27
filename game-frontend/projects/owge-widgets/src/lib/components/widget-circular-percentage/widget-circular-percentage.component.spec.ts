import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetCircularPercentageComponent } from './widget-circular-percentage.component';

describe('WidgetCircularPercentageComponent', () => {
  let component: WidgetCircularPercentageComponent;
  let fixture: ComponentFixture<WidgetCircularPercentageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetCircularPercentageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetCircularPercentageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
