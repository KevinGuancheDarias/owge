import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ListJoinRequestComponent } from './list-join-request.component';

describe('ListJoinRequestComponent', () => {
  let component: ListJoinRequestComponent;
  let fixture: ComponentFixture<ListJoinRequestComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ListJoinRequestComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ListJoinRequestComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
