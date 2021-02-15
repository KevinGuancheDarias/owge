import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { WidgetSortListComponent } from './widget-sort-list.component';

describe('WidgetSortListComponent', () => {
  let component: WidgetSortListComponent;
  let fixture: ComponentFixture<WidgetSortListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ WidgetSortListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(WidgetSortListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
