import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe, timer, Subscription} from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import * as moment from 'moment';

import { NGXLogger } from 'ngx-logger';
import { UserService } from './user/user.service';


import { Environment } from './model/environment';
import { ServerTime } from './model/settings';
import { Bet } from './model/bet';


@Injectable({
  providedIn: 'root'
})
export class BetterTimerService {
  private startupTime = new Date()
  private currentTime = new Date()
  private gamesStarts = new Date()

  //should we fetch the time from the server or take the interactively set time
  private timeFromServer = true
  private DF = 'd/M HH:mm'


  //time before game start that bet closes
  private MSTOCLOSING = 60 * 60 * 1000 //in ms

  //time to update clock
  private UPDATEINTERVAL = 1000 * 5 //in ms

  //reload clock from server
  private RESETTIMEDIFF = 5 * 60 * 1000 //in ms

  private timer = timer(this.UPDATEINTERVAL,this.UPDATEINTERVAL)
  private subscription: Subscription;

  constructor(private logger: NGXLogger, private http: HttpClient, private userService: UserService) {
     this.init()
  }

  private init(){
     this.subscription = this.timer.subscribe( tick =>
         this.updateTimeTick()
     )
  }

  private updateTimeTick(){
     this.currentTime = new Date(this.getTimeMillis() + this.UPDATEINTERVAL)
     const timerunning = this.getTimeMillis() - this.startupTime.getTime();
     if (timerunning > this.RESETTIMEDIFF && this.timeFromServer) {
          this.updateTimeFromServer();
     }
  }

  getTime(): Date {
    return this.currentTime
  }

  getTimeMillis(): number {
    return this.getTime().getMilliseconds()
  }

  getDateFormatter(): string {
    return this.DF
  }

  diffToStart(serverStart: number): number {
     return (serverStart - this.MSTOCLOSING) - this.getTimeMillis()
  }

  timeLeft(serverStart: number): string {
     //boolean true add in/ago
     //negative values = ago
     //positive values = in
     const s = moment.duration(this.diffToStart(serverStart), "milliseconds").humanize(false)
     return s
  }

  specialBetOpen(bet: Bet): boolean {
      return this.canBet(this.gamesStarts, bet)
  }

  specialBetsOpen(): boolean {
     return ! this.betClosed(this.gamesStarts.getMilliseconds())
  }

  betClosed(serverStart: number): boolean {
      return this.diffToStart(serverStart) < 0
  }

  canBet(serverStart, bet): boolean {
      const diff = this.diffToStart(serverStart)
      const owner = this.userService.isOwner(bet.userId)
      return diff > 0 && owner
  }


  //for testing:
  updateDate(date: Date) {
        const olddate = this.getTime()
        this.timeFromServer = false
        const nm = moment(date)
        const om = moment(this.getTime())
        nm.hours(om.hours())
        nm.minutes(om.minutes())
        nm.seconds(om.seconds())
        this.currentTime = nm.toDate()
        this.logger.info(`updated date ${olddate} => ${this.getTime()}`)
        this.updateTimeOnServer(this.getTime())
  }
  //for testing
  updateTime(time: Date) {
        const oldtime = this.getTime()
        this.timeFromServer = false
        const nm = moment(time)
        const om = moment(this.getTime())
        om.hours(nm.hours())
        om.minutes(nm.minutes())
        this.currentTime = om.toDate()
        this.logger.info(`updated time ${oldtime} => ${this.getTime()}`)
        this.updateTimeOnServer(this.getTime())
  }

  private setTimeFromServer(date: Date){
      this.startupTime = date
      this.currentTime = this.startupTime
      this.logger.debug(`updated time from server ${moment(this.getTime()).format()}`)
  }

  updateTimeFromServer() {
        this.http.get<ServerTime>(Environment.api("time")).pipe(
           tap(_ => this.logger.debug(`updating time from server`)),
           catchError(this.handleError<ServerTime>(`update time from server`))
        ).subscribe( data =>
           this.setTimeFromServer(data['serverTime'])
        )
  }

  updateTimeOnServer(time: Date) {
        this.http.post<ServerTime>(Environment.api("time"),{serverTime: time.getTime()}).pipe(
              tap(_ => this.logger.debug(`updating time from server`)),
              catchError(this.handleError<ServerTime>(`update time from server`))
        ).subscribe( data =>
            this.updateTimeFromServer()
        )
  }

  resetTime(){
        this.http.post<any>(Environment.api("time/reset"), {}).pipe(
            tap(_ => this.logger.debug(`resetting time on server`)),
            catchError(this.handleError<any>(`resetting time from server`))
         ).subscribe( data => {
            this.timeFromServer = true
            this.updateTimeFromServer()
         }
      )
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
