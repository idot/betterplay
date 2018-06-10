import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';

import { Observable ,  of, EMPTY } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';

import { Environment, ErrorMessage, OkMessage } from '../model/environment';
import { User, FilterSettings } from '../model/user';
import { ToastComponent } from '../components/toast/toast.component';
import { MatSnackBar } from '@angular/material';
import { BetterdbService } from '../betterdb.service';
import { FilterService } from './filter.service';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};


@Injectable()
export class UserService {



  private user: User | null = null;

  constructor(private logger: NGXLogger, private snackBar: MatSnackBar, private http: HttpClient, private filterService: FilterService) {
    logger.info("user service ctor")
  //  this.reauthenticate() //for browser refresh
  //   this.query()
  }

  isOwner(id: number): boolean {
     return this.user && this.user.id == id || false;
  }

  isIdentical(username: string): boolean {
     return this.user && this.user.username == username || false;
  }

  isAdmin(): boolean {
    return this.user && this.user.isAdmin || false;
  }

  getUser(): User | null {
    return this.user
  }

  isLoggedIn(): boolean {
    return this.user != null
  }

  login(username: string, password: string) {
      return this.http.post<any>(Environment.api("login"), { username: username, password: password })
         .pipe(
            tap(_ => this.logger.debug(`fetching user with username ${username}`)),
             catchError(this.handleSpecificError('login'))
      )
   }



 /**
  * set by login component after successful this.login()
  *
  **/
  setUserData(data: {}){
    this.user = data['user']
    this.filterService.setFilter( this.user ? this.user.filterSettings : this.filterService.getDefaultFilter())
    localStorage.setItem(Environment.XAUTHTOKEN,  data[Environment.XAUTHTOKEN])
    this.logger.debug(`fetched user ${this.user}`)
  }

  logout() {
      this.http.post<any>(Environment.api("logout"), {}).subscribe()
      localStorage.removeItem(Environment.XAUTHTOKEN)
      this.user = null
  }

  query(){
  this.http.post<any>("nowhere", {})
     .pipe(
      tap(_ => console.error('query')),
      catchError((err: any) => { return of("stop") })
     ).subscribe( data => {
         console.error('subscribed')
       }
     )
  }


  reauthenticate(): Promise<any> {
    return new Promise((resolve, reject) => {
        if(localStorage.getItem(Environment.XAUTHTOKEN)){//is in interceptor
            this.http.get<any>(Environment.api("ping")).toPromise().then(
              success => {
                if(success['user']){
                   this.user = success['user']
                   this.filterService.setFilter( this.user ? this.user.filterSettings : this.filterService.getDefaultFilter())
                }
                resolve()
              },
              error => {
                resolve()
                this.snackBar.openFromComponent(ToastComponent, { data: { message: "session expired, please log in again", level: "error"}})
              }
          )
        } else {
            resolve()
        }
    })
  }


  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private  handleSpecificError(operation: string) {
         return (error: HttpErrorResponse): Observable<ErrorMessage> => {
            if (error instanceof ErrorEvent) {
               this.snackBar.openFromComponent(ToastComponent, { data: { message: `${operation} failed client side: ${error.message}`, level: "error"}})
               this.logger.error(`${operation} failed client side: ${error.message}`)
               return of({ error: error.message, type: 'unknown' })
            } else {
                if(error['error']){
                   this.snackBar.openFromComponent(ToastComponent, { data: { message: error['error'], level: "error"}})
                } else {
                   this.snackBar.openFromComponent(ToastComponent, { data: { message: error.message, level: "error"}})
                }
                return of({ error: "could not log you in", type: 'unknown' })
            }
         }
    }


}
