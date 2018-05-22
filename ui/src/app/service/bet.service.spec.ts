import { TestBed, inject } from '@angular/core/testing';

import { BetService } from './bet.service';

describe('BetServiceService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BetService]
    });
  });

  it('should be created', inject([BetService], (service: BetService) => {
    expect(service).toBeTruthy();
  }));
});
