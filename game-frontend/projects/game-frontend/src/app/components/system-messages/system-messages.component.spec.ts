import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SystemMessagesComponent } from './system-messages.component';

describe('SystemMessagesComponent', () => {
  let component: SystemMessagesComponent;
  let fixture: ComponentFixture<SystemMessagesComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SystemMessagesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SystemMessagesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
