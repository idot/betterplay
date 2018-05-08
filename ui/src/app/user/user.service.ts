import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';

import { Observable ,  of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';

import { Environment } from '../model/environment';
import { User } from '../model/user';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};


@Injectable()
export class UserService {

  private user: User | null = null;
  private authtoken = "";
//  private a = environment.AUTHTOKEN;

  constructor(private logger: NGXLogger, private http: HttpClient) {}

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


  //TODO: redirect if user did not have instructions
  login(username: string, password: string): User | null {
      this.http.post<any>(Environment.api("login"), { username: username, password: password })
         .pipe(
            tap(_ => this.logger.debug(`fetching user with username ${username}`)),
             catchError(this.handleError('login'))
         ).subscribe( data => {
             this.logger.debug(data);
             this.user = data['user'];
             this.authtoken = data[Environment.AUTHTOKEN];
             localStorage.setItem(Environment.AUTHTOKEN, this.authtoken); //add expiry
           }
        )
       return this.user;
   }




  logout() {
      localStorage.removeItem(Environment.AUTHTOKEN);
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
