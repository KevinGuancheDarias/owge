import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SynchronizeCredentialsComponent } from './synchronize-credentials.component';

describe('SynchronizeCredentialsComponent', () => {
  let component: SynchronizeCredentialsComponent;
  let fixture: ComponentFixture<SynchronizeCredentialsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SynchronizeCredentialsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SynchronizeCredentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
