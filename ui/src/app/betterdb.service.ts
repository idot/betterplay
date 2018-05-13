import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { Environment } from './model/environment';
import { Bet, UserWithBets, Game } from './model/bet';
import { User } from './model/user';
import { catchError, tap, map } from 'rxjs/operators';


@Injectable({
  providedIn: 'root'
})
export class BetterdbService {


  constructor(private logger: NGXLogger, private http: HttpClient) { }


  isDebug(): boolean {
    return true
  }

  /**
  * converts json response date which are strings in Angular to Date
  **/
  gameMapper(game: Game): Game {
     if(typeof(game.serverStart) === "string"){
        game.serverStart = new Date(game.serverStart)
     }
     if(typeof(game.localStart) === "string"){
        game.localStart = new Date(game.localStart)
     }
     return game
  }

  getBetsForUser(username: string): Observable<UserWithBets> {
    return this.http.get<UserWithBets>(Environment.api(`user/${username}`))
       .pipe(
          tap(_ => this.logger.debug(`fetching UserWithBets with username ${username}`)),
          map(u => {
              u.gameBets.forEach(gb => this.gameMapper(gb.game.game))
              return u
            }
          ),
          catchError(this.handleError<UserWithBets>(`getBetsForUser ${username}`))
       );
  }


  badgecolour(rank): string {
      switch (rank) {
         case 1:
            return "badge-gold";
         case 2:
            return "badge-silver";
         case 3:
            return "badge-bronze";
        default:
            return "badge-points";
      }
  };

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
   handleError<T> (operation = 'operation', result?: T) {
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
