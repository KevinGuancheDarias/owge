import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfigurationCrudComponent } from './configuration-crud.component';

describe('ConfigurationCrudComponent', () => {
  let component: ConfigurationCrudComponent;
  let fixture: ComponentFixture<ConfigurationCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ConfigurationCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ConfigurationCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
