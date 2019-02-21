import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RankingDisplayComponent } from './ranking-display.component';

describe('RankingDisplayComponent', () => {
  let component: RankingDisplayComponent;
  let fixture: ComponentFixture<RankingDisplayComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RankingDisplayComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RankingDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
