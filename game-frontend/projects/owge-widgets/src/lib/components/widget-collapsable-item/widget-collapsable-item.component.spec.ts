import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetCollapsableItemComponent } from './widget-collapsable-item.component';

describe('WidgetCollapsableItemComponent', () => {
  let component: WidgetCollapsableItemComponent;
  let fixture: ComponentFixture<WidgetCollapsableItemComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetCollapsableItemComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetCollapsableItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
