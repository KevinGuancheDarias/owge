import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplaySimpleItemComponent } from './widget-display-simple-item.component';

describe('WidgetDisplaySimpleItemComponent', () => {
  let component: WidgetDisplaySimpleItemComponent;
  let fixture: ComponentFixture<WidgetDisplaySimpleItemComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetDisplaySimpleItemComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplaySimpleItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
