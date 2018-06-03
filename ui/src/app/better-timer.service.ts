import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';
import { Observable, of, pipe, timer, Subscription} from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import * as moment from 'moment';

import { NGXLogger } from 'ngx-logger';

import { Environment } from './model/environment';
import { ServerTime } from './model/settings';
import { Bet } from './model/bet';


@Injectable({
  providedIn: 'root'
})
export class BetterTimerService {
  private startupTime = new Date()
  private currentTime = new Date()

  //should we fetch the time from the server or take the interactively set time
  private timeFromServer = true
  private DF = 'd/M HH:mm'


  //time to update clock
  private UPDATEINTERVAL = 1000 * 5 //in ms

  //reload clock from server
  private RESETTIMEDIFF = 5 * 60 * 1000 //in ms

  private timer = timer(this.UPDATEINTERVAL,this.UPDATEINTERVAL)
  private subscription: Subscription;

  constructor(private logger: NGXLogger, private http: HttpClient) {
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
    return this.getTime().getTime()
  }

  getDateFormatter(): string {
    return this.DF
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
  updateTime(hour: number, minute: number) {
        const oldtime = this.getTime()
        this.timeFromServer = false
        const om = moment(this.getTime())
        om.hours(hour)
        om.minutes(minute)
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
           this.setTimeFromServer(new Date(data['serverTime']))
        )
  }

  updateTimeOnServer(time: Date) {
        this.http.post<ServerTime>(Environment.api("time"),{serverTime: time.toISOString()}).pipe(
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

  setDateFormat(df: string){
    this.DF = df
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
