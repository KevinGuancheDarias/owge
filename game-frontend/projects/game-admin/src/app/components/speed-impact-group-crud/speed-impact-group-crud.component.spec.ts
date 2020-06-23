import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SpeedImpactGroupCrudComponent } from './speed-impact-group-crud.component';

describe('SpeedImpactGroupCrudComponent', () => {
  let component: SpeedImpactGroupCrudComponent;
  let fixture: ComponentFixture<SpeedImpactGroupCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SpeedImpactGroupCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpeedImpactGroupCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
