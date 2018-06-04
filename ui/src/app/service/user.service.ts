import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';

import { Observable ,  of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';

import { Environment, ErrorMessage, OkMessage } from '../model/environment';
import { User } from '../model/user';
import { ToastComponent } from '../components/toast/toast.component';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};


@Injectable()
export class UserService {

  private user: User | null = null;
  private authtoken = ""


  constructor(private logger: NGXLogger, private http: HttpClient) {
    console.trace("created user service")
  }

  getAuthorizationToken(): string {
     return this.authtoken;
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
    //console.debug(`isloggedin ${this.user}`)
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
    this.authtoken = data[Environment.AUTHTOKEN]
    this.user = data['user']
    localStorage.setItem(Environment.AUTHTOKEN, this.authtoken)//TODO: add expiry
    this.logger.debug(`fetched user ${this.user}`)
  }

  logout() {
      localStorage.removeItem(Environment.AUTHTOKEN)
      this.authtoken = ""
      this.user = null
  }

  reauthenticate() {
    //in the previous version opening a new window led to a loss of login in the new window
  }

  /**
   * Handle Http operation that failed.
   * Let the app continue.
   * @param operation - name of the operation that failed
   * @param result - optional value to return as the observable result
   */
  private  handleSpecificError(operation: string) {
       return (error: HttpErrorResponse): Observable<ErrorMessage> => {
            if (error.error instanceof ErrorEvent) {
            // A client-side or network error occurred. Handle it accordingly.
            this.logger.error(`${operation} failed client side: ${error.error.message}`)
            return of({ error: error.error.message, type: 'unknown' })
        } else {
            return of({ error: "could not log you in", type: 'unknown' })
        }
     }
  }

}
