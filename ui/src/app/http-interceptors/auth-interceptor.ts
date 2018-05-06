import { Injectable } from '@angular/core';
import {
  HttpEvent, HttpInterceptor, HttpHandler, HttpRequest
} from '@angular/common/http';

import { Observable } from 'rxjs/Observable';

import { UserService } from '../user/user.service';
import { Environment } from '../model/environment';


@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private userService: UserService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {

    const authToken = this.userService.getAuthorizationToken();

    const authReq = req.clone({
      headers: req.headers.set(Environment.AUTHTOKEN, authToken)
    });

    return next.handle(authReq);
  }
}
