import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplayRuleTableComponent } from './widget-display-rule-table.component';

describe('WidgetDisplayRuleTableComponent', () => {
  let component: WidgetDisplayRuleTableComponent;
  let fixture: ComponentFixture<WidgetDisplayRuleTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ WidgetDisplayRuleTableComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplayRuleTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
