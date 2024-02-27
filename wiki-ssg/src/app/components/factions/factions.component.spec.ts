import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FactionsComponent } from './factions.component';

describe('FactionsComponent', () => {
  let component: FactionsComponent;
  let fixture: ComponentFixture<FactionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FactionsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(FactionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
