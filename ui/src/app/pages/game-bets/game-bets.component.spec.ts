import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { GameBetsComponent } from './game-bets.component';

describe('GameBetsComponent', () => {
  let component: GameBetsComponent;
  let fixture: ComponentFixture<GameBetsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ GameBetsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(GameBetsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
