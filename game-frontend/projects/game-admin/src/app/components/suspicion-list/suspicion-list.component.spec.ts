import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SuspicionListComponent } from './suspicion-list.component';

describe('SuspicionListComponent', () => {
  let component: SuspicionListComponent;
  let fixture: ComponentFixture<SuspicionListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SuspicionListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SuspicionListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
