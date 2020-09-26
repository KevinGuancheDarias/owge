import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetChooseItemModalComponent } from './widget-choose-item-modal.component';

describe('WidgetChooseItemModalComponent', () => {
  let component: WidgetChooseItemModalComponent;
  let fixture: ComponentFixture<WidgetChooseItemModalComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetChooseItemModalComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetChooseItemModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
