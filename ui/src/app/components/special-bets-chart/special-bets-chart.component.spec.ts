import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialBetsChartComponent } from './special-bets-chart.component';

describe('SpecialBetsChartComponent', () => {
  let component: SpecialBetsChartComponent;
  let fixture: ComponentFixture<SpecialBetsChartComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SpecialBetsChartComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SpecialBetsChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
