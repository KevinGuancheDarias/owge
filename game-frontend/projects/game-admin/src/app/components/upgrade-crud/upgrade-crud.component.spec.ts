import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { UpgradeCrudComponent } from './upgrade-crud.component';

describe('UpgradeCrudComponent', () => {
  let component: UpgradeCrudComponent;
  let fixture: ComponentFixture<UpgradeCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ UpgradeCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UpgradeCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
