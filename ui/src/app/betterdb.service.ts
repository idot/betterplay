import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { Environment } from './model/environment';
import { Bet, UserWithBets } from './model/bet';
import { User } from './model/user';
import { catchError, tap } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class BetterdbService {


  constructor(private logger: NGXLogger, private http: HttpClient) { }

  canBet(serverStart, bet): boolean {
  //  var diff = (serverStart - MSTOCLOSING) - vm.currentTime;
    return true
  }

  getBetsForUser(username: string): Observable<UserWithBets> {
    return this.http.get<UserWithBets>(Environment.api(`user/${username}`))
       .pipe(
          tap(_ => this.logger.debug(`fetching UserWithBets with username ${username}`)),
          catchError(this.handleError<UserWithBets>(`getBetsForUser ${username}`))
       );
  }


  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private handleError<T> (operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {

      // TODO: send the error to remote logging infrastructure
      //console.error(error); // log to console instead

      // TODO: better job of transforming error for user consumption
      this.logger.error(`${operation} failed: ${error.message}`);

      // Let the app keep running by returning an empty result.
      return of(result as T);
    };
  }
}
