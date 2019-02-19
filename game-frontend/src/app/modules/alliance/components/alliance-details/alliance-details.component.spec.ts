import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AllianceDetailsComponent } from './alliance-details.component';

describe('AllianceDetailsComponent', () => {
  let component: AllianceDetailsComponent;
  let fixture: ComponentFixture<AllianceDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AllianceDetailsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AllianceDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
