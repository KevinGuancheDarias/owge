import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OwgeFactionComponent } from './owge-faction.component';

describe('OwgeFactionComponent', () => {
  let component: OwgeFactionComponent;
  let fixture: ComponentFixture<OwgeFactionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OwgeFactionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OwgeFactionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
