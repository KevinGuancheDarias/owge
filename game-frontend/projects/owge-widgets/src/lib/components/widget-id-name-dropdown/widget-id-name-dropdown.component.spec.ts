import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetIdNameDropdownComponent } from './widget-id-name-dropdown.component';

describe('WidgetIdNameDropdownComponent', () => {
  let component: WidgetIdNameDropdownComponent;
  let fixture: ComponentFixture<WidgetIdNameDropdownComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetIdNameDropdownComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetIdNameDropdownComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
