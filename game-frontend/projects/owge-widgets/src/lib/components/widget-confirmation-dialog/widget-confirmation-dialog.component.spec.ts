import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetConfirmationDialogComponent } from './widget-confirmation-dialog.component';

describe('WidgetConfirmationDialogComponent', () => {
  let component: WidgetConfirmationDialogComponent;
  let fixture: ComponentFixture<WidgetConfirmationDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetConfirmationDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetConfirmationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
