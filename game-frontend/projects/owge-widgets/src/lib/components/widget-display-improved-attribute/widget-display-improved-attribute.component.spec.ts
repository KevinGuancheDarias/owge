import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplayImprovedAttributeComponent } from './widget-display-improved-attribute.component';

describe('WidgetDisplayImprovedAttributeComponent', () => {
  let component: WidgetDisplayImprovedAttributeComponent;
  let fixture: ComponentFixture<WidgetDisplayImprovedAttributeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetDisplayImprovedAttributeComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplayImprovedAttributeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
