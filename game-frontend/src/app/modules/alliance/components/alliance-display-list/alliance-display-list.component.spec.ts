import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AllianceDisplayListComponent } from './alliance-display-list.component';

describe('AllianceDisplayListComponent', () => {
  let component: AllianceDisplayListComponent;
  let fixture: ComponentFixture<AllianceDisplayListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AllianceDisplayListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AllianceDisplayListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
