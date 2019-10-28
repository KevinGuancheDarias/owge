import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ObjectImprovementsCrudComponent } from './object-improvements-crud.component';

describe('ObjectImprovementsCrudComponent', () => {
  let component: ObjectImprovementsCrudComponent;
  let fixture: ComponentFixture<ObjectImprovementsCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ObjectImprovementsCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectImprovementsCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
