import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';

import { Observable } from 'rxjs/Observable';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';
import { of } from 'rxjs/observable/of';
import { catchError, map, tap } from 'rxjs/operators';

import { Environment } from '../model/environment';
import { User } from '../model/user';
import { environment } from '../../environments/environment';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};


@Injectable()
export class UserService {

  private user: User | null = null;
  private authtoken = "";
//  private a = environment.AUTHTOKEN;

  constructor(private http: HttpClient) {}

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
            tap(_ => console.log(`fetching user with username ${username}`)),
             catchError(this.handleError)
         ).subscribe( data => {
             console.log(data);
             this.user = data['user'];
             this.authtoken = data[Environment.AUTHTOKEN];
           }
        )
       return this.user;
   }




  logout() {

  }

  reauthenticate() {
    //in the previous version opening a new window led to a loss of login in the new window
  }

  private handleError(error: HttpErrorResponse) {
      if (error.error instanceof ErrorEvent) {
        // A client-side or network error occurred. Handle it accordingly.
        console.error('An error occurred:', error.error.message);
      } else {
        // The backend returned an unsuccessful response code.
        // The response body may contain clues as to what went wrong,
        console.error(
          `Backend returned code ${error.status}, ` +
          `body was: ${error.error}`);
      }
      // return an ErrorObservable with a user-facing error message
      return new ErrorObservable(
        'Something bad happened; please try again later.');
 };

}
