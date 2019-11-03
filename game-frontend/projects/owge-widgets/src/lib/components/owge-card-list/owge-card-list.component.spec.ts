import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { OwgeCardListComponent } from './owge-card-list.component';

describe('OwgeCardListComponent', () => {
  let component: OwgeCardListComponent;
  let fixture: ComponentFixture<OwgeCardListComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ OwgeCardListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(OwgeCardListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
