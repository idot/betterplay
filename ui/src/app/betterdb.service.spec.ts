import { TestBed, inject } from '@angular/core/testing';

import { BetterdbService } from './betterdb.service';

describe('BetterdbService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BetterdbService]
    });
  });

  it('should be created', inject([BetterdbService], (service: BetterdbService) => {
    expect(service).toBeTruthy();
  }));
});
