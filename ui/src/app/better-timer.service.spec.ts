import { TestBed, inject } from '@angular/core/testing';

import { BetterTimerService } from './better-timer.service';

describe('BetterTimerService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BetterTimerService]
    });
  });

  it('should be created', inject([BetterTimerService], (service: BetterTimerService) => {
    expect(service).toBeTruthy();
  }));
});
