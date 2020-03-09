import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FactionCrudComponent } from './faction-crud.component';

describe('FactionCrudComponent', () => {
  let component: FactionCrudComponent;
  let fixture: ComponentFixture<FactionCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FactionCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FactionCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
