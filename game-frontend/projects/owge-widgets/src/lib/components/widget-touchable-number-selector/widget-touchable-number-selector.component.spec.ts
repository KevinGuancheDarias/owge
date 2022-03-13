import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetTouchableNumberSelectorComponent } from './widget-touchable-number-selector.component';

describe('WidgetTouchableNumberSelectorComponent', () => {
  let component: WidgetTouchableNumberSelectorComponent;
  let fixture: ComponentFixture<WidgetTouchableNumberSelectorComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WidgetTouchableNumberSelectorComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetTouchableNumberSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
