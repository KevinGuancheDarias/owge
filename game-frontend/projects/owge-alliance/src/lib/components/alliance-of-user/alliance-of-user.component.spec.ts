import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AllianceOfUserComponent } from './alliance-of-user.component';

describe('AllianceOfUserComponent', () => {
  let component: AllianceOfUserComponent;
  let fixture: ComponentFixture<AllianceOfUserComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AllianceOfUserComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AllianceOfUserComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
