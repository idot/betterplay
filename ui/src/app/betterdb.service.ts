import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { Environment, ErrorMessage } from './model/environment';
import { Bet, UserWithBets, Game, Player, PlayerWithTeam, Team, GameWithTeams } from './model/bet';
import { User, UserWSpecialBets, GameWithBetsUsers } from './model/user';
import { catchError, tap, map } from 'rxjs/operators';
import { BetterSettings } from './model/settings';
import { BetterTimerService } from './better-timer.service';
import { SpecialBet, SpBet, SpecialBetPredictions } from './model/specialbet';
import { HttpErrorResponse } from '@angular/common/http';






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

  saveSpecialBetPrediction(user: User, sp: SpecialBet): Observable<User | ErrorMessage>{
      return this.http.post<any>(Environment.api(`specialBet`), sp.bet).pipe(
        catchError(this.handleSpecificError(`saving special bet prediction`))
      )
  }

  saveSpecialBetResult(user: User, sp: SpecialBet): Observable<User | ErrorMessage>{
    return this.http.post<any>(Environment.api(`specialBetResult`), sp.bet).pipe(
      catchError(this.handleSpecificError(`saving special bet result`))
    )
  }

  createUser(details: {}) {
    return this.http.put<any>(Environment.api(`user/create`), details).pipe(
      catchError(this.handleSpecificError(`creating user`))
    )
  }

  constructor(private logger: NGXLogger, private http: HttpClient, private timeService: BetterTimerService) {
     this.init()
  }


  init(){
    this.getSettingsForService()
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

  getSettings(): BetterSettings {
     return this.settings
  }

  getGames(): Observable<GameWithTeams[]>{
    return this.http.get<GameWithTeams[]>(Environment.api(`games`))
       .pipe(
          tap(_ => this.logger.debug(`fetching games with teams`)),
          map(gs => {
              gs.forEach(g => this.gameMapper(g.game))
              return gs
            }
          ),
          catchError(this.handleError<GameWithTeams[]>(`getGamesWithTeams`))
       )
  }

  getGameWithBetsForUsers(nr: number): Observable<GameWithBetsUsers> {
    return this.http.get<GameWithBetsUsers>(Environment.api(`game/${nr}`))
       .pipe(
          tap(_ => this.logger.debug(`fetching game ${nr} with betsUsers`)),
          map(g => {
                g.game.game = this.gameMapper(g.game.game)
                return g
             }
          ),
          catchError(this.handleError<GameWithBetsUsers>(`getGameWithBetsUsers`))
       )
  }

  getSpecialBetStats(templateName: string): Observable<SpecialBetPredictions>{
    return this.http.get<SpecialBetPredictions>(Environment.api(`statistics/specialBet/${templateName}`))
       .pipe(
          tap(_ => this.logger.debug(`fetching special bet stats ${templateName}`)),
          catchError(this.handleError<SpecialBetPredictions>(`getSpecialBetStats`))
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

  changePasswordRequest(req: {}): Observable<string> {
    return this.http.get<any>(Environment.api(`changePasswordRequest`))
  }

  private getSettingsForService() {
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
   handleSpecificError(operation: string) {
      return (error: HttpErrorResponse): Observable<ErrorMessage> => {
           if (error.error instanceof ErrorEvent) {
           // A client-side or network error occurred. Handle it accordingly.
           this.logger.error(`${operation} failed client side: ${error.error.message}`)
           return of({ error: error.error.message, type: 'unknown' })
       } else {
           // The backend returned an unsuccessful response code.
           // The response body may contain clues as to what went wrong,
           this.logger.error(`backend returned code ${error.status}`, `body was: ${error.error}`)
           if(error.error.error && error.error.error.indexOf("Unique index or primary key violation") >= 0){
              return of({ error: 'could not create', type: 'not unique' })
           } else {
              return of({ error: error.error, type: 'unknown' })
           }
       }
    }
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
   handleError<T> (operation = 'operation', result?: T) {
    return (error: HttpErrorResponse): Observable<T> => {
      if (error.error instanceof ErrorEvent) {
     // A client-side or network error occurred. Handle it accordingly.
       this.logger.error(`${operation} failed client side: ${error.error.message}`)
     } else {
       // The backend returned an unsuccessful response code.
       // The response body may contain clues as to what went wrong,
       this.logger.error(`backend returned code ${error.status}`, `body was: ${error.error}`)

     }
      return of(result as T);
    }
  }
}
