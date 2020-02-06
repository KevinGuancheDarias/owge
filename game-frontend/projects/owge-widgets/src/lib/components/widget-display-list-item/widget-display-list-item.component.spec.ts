import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplayListItemComponent } from './widget-display-list-item.component';

describe('WidgetDisplayListItemComponent', () => {
  let component: WidgetDisplayListItemComponent;
  let fixture: ComponentFixture<WidgetDisplayListItemComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetDisplayListItemComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplayListItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
