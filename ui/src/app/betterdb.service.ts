import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { Environment } from './model/environment';
import { Bet, UserWithBets, Game, Player, PlayerWithTeam, Team } from './model/bet';
import { User, UserWSpecialBets } from './model/user';
import { catchError, tap, map } from 'rxjs/operators';
import { BetterSettings } from './model/settings';
import { BetterTimerService } from './better-timer.service';
import { SpecialBet, SpBet } from './model/specialbet';


@Injectable({
  providedIn: 'root'
})
export class BetterdbService {
  private settings: BetterSettings = {
    debug: false,
    gamesStarts: new Date(),
    recaptchasite: ""
  }

  getSpecialBetForUser(user: User | null, betId: number): Observable<SpecialBet> {
    if(user){
      return this.http.get<UserWSpecialBets>(Environment.api(`user/${user.username}/specialBets`))
         .pipe(
            tap(_ => this.logger.debug(`fetching specialbets`)),
            map( ubets => {
              const filtered = ubets.templateBets.filter(bet => bet.bet.id == betId)
              if(filtered.length == 1){
                  this.logger.debug(`found one bet ${betId} for user ${user.username}`)
                  return filtered[0]
              } else {
                  throw Error(`could not find bet ${betId} for user ${user.username}`)
              }
           }),
          catchError(this.handleError<SpecialBet>(`getSpecialBetForUser`))
        )
    } else {
       throw Error(`user not logged in`)
    }
  }

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(Environment.api(`users`))
      .pipe(
        tap(_ => this.logger.debug(`fetching users`)),
        catchError(this.handleError<User[]>(`getUsers`))
      )
  }

  saveSpecialBetPrediction(user: User, sp: SpecialBet){
      this.http.post<any>(Environment.api(`specialBet`), sp.bet).pipe(
        catchError(this.handleError<any>(`saving special bet`))
        ).subscribe( data => {
            this.logger.log(data)
            return data
        }
      )
  }

  saveSpecialBetResult(user: User, sp: SpecialBet){
    this.http.post<any>(Environment.api(`specialBetResult`), sp.bet).pipe(
      catchError(this.handleError<any>(`saving special bet`))
      ).subscribe( data => {
          this.logger.log(data)
          return data
      }
    )
  }

  createUser(details: {}){
    this.http.put<any>(Environment.api(`user/create`), details).pipe(
      catchError(this.handleError<any>(`creating user`))
      ).subscribe( data => {
          this.logger.log(data)
          return data
      }
    )
  }

  constructor(private logger: NGXLogger, private http: HttpClient, private timeService: BetterTimerService) {
     this.getSettings()
  }


  init(){
    this.getSettings()
  }

  isDebug(): boolean {
    return this.settings && this.settings.debug || false
  }

  getGamesStart(): Date {
    return this.settings && this.settings.gamesStarts || new Date()
  }

  getPlayers(): Observable<PlayerWithTeam[]> {
    return this.http.get<PlayerWithTeam[]>(Environment.api(`players`))
       .pipe(
          tap(_ => this.logger.debug(`fetching players`)),
          catchError(this.handleError<PlayerWithTeam[]>(`getPlayers`))
       )
  }

  getTeams(): Observable<Team[]> {
    return this.http.get<Team[]>(Environment.api(`teams`))
       .pipe(
          tap(_ => this.logger.debug(`fetching teams`)),
          catchError(this.handleError<Team[]>(`getTeams`))
       )
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
       )
  }

  getSettings() {
    this.http.get<BetterSettings>(Environment.api('settings'))
      .pipe(
        tap(_ => this.logger.debug(`fetching settings`),
        catchError(this.handleError<BetterSettings>(`getSettings`))
        ),
        map(s => {
            s.gamesStarts = new Date(s.gamesStarts)
            return s
          }
        ))
      .subscribe(s => {
        this.settings = s
      })
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
  }




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
