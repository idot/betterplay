import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BetViewComponent } from './bet-view.component';

describe('BetViewComponent', () => {
  let component: BetViewComponent;
  let fixture: ComponentFixture<BetViewComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ BetViewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(BetViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
