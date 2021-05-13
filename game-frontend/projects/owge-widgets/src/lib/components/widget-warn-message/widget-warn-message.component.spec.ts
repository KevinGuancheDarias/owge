import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetWarnMessageComponent } from './widget-warn-message.component';

describe('WidgetWarnMessageComponent', () => {
  let component: WidgetWarnMessageComponent;
  let fixture: ComponentFixture<WidgetWarnMessageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WidgetWarnMessageComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetWarnMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
