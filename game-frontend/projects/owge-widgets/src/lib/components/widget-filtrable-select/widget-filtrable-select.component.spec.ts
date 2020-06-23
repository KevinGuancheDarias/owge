import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetFiltrableSelectComponent } from './widget-filtrable-select.component';

describe('WidgetFiltrableSelectComponent', () => {
  let component: WidgetFiltrableSelectComponent;
  let fixture: ComponentFixture<WidgetFiltrableSelectComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetFiltrableSelectComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetFiltrableSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
