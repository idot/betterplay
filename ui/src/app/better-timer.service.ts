import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

@Injectable({
  providedIn: 'root'
})
export class BetterTimerService {
  private startupTime = new Date()
  private currentTime = new Date()

  private gamesStarts = new Date()

  getTime(): Date {
    return this.currentTime;
  }



  constructor(private logger: NGXLogger, private http: HttpClient) { }
}
