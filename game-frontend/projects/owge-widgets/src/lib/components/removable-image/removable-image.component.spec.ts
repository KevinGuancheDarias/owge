import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RemovableImageComponent } from './removable-image.component';

describe('RemovableImageComponent', () => {
  let component: RemovableImageComponent;
  let fixture: ComponentFixture<RemovableImageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RemovableImageComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RemovableImageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
