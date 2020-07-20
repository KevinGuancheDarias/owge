import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AttackRuleCrudComponent } from './attack-rule-crud.component';

describe('AttackRuleCrudComponent', () => {
  let component: AttackRuleCrudComponent;
  let fixture: ComponentFixture<AttackRuleCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AttackRuleCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AttackRuleCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
