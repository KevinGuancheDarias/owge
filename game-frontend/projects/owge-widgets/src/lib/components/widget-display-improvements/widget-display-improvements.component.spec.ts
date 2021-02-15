import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetDisplayImprovementsComponent } from './widget-display-improvements.component';

describe('WidgetDisplayImprovementsComponent', () => {
  let component: WidgetDisplayImprovementsComponent;
  let fixture: ComponentFixture<WidgetDisplayImprovementsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetDisplayImprovementsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetDisplayImprovementsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
