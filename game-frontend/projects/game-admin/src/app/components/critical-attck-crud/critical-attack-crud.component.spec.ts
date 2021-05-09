import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CriticalAttackCrudComponent } from './critical-attack-crud.component';

describe('CriticalAttackCrudComponent', () => {
  let component: CriticalAttackCrudComponent;
  let fixture: ComponentFixture<CriticalAttackCrudComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CriticalAttackCrudComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CriticalAttackCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
