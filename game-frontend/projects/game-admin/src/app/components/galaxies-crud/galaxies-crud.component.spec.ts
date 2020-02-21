import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { GalaxiesCrudComponent } from './galaxies-crud.component';

describe('GalaxiesCrudComponent', () => {
  let component: GalaxiesCrudComponent;
  let fixture: ComponentFixture<GalaxiesCrudComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ GalaxiesCrudComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GalaxiesCrudComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
