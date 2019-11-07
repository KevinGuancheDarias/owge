import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetSpanWithPlaceholderComponent } from './widget-span-with-placeholder.component';

describe('WidgetSpanWithPlaceholderComponent', () => {
  let component: WidgetSpanWithPlaceholderComponent;
  let fixture: ComponentFixture<WidgetSpanWithPlaceholderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetSpanWithPlaceholderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetSpanWithPlaceholderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
