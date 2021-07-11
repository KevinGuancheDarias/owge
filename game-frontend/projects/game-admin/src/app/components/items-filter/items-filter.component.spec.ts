import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ItemsFilterComponent } from './items-filter.component';

describe('ItemsFilterComponent', () => {
  let component: ItemsFilterComponent;
  let fixture: ComponentFixture<ItemsFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ItemsFilterComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ItemsFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
