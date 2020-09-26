import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TranslatableComponent } from './translatable.component';

describe('TranslatableComponent', () => {
  let component: TranslatableComponent;
  let fixture: ComponentFixture<TranslatableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TranslatableComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TranslatableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
