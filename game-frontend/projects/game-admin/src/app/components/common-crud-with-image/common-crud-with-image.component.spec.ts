import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CommonCrudWithImageComponent } from './common-crud-with-image.component';

describe('CommonCrudWithImageComponent', () => {
  let component: CommonCrudWithImageComponent;
  let fixture: ComponentFixture<CommonCrudWithImageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CommonCrudWithImageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CommonCrudWithImageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
