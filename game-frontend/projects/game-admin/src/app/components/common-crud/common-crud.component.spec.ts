import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CommonCrudComponent } from './common-crud.component';

describe('CommonCrudComponent', () => {
  let component: CommonCrudComponent;
  let fixture: ComponentFixture<CommonCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CommonCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommonCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
