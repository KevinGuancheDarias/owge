import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialLocationCrudComponent } from './special-location-crud.component';

describe('SpecialLocationCrudComponent', () => {
  let component: SpecialLocationCrudComponent;
  let fixture: ComponentFixture<SpecialLocationCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SpecialLocationCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpecialLocationCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
