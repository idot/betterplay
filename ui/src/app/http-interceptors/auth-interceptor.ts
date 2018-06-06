import { Injectable } from '@angular/core';
import {
  HttpEvent, HttpInterceptor, HttpHandler, HttpRequest
} from '@angular/common/http';

import { Observable } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { UserService } from '../service/user.service';
import { Environment } from '../model/environment';


@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private logger: NGXLogger, private userService: UserService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {


    const authToken = localStorage.getItem(Environment.XAUTHTOKEN)
    if(authToken){
      const authReq = req.clone({
        headers: req.headers.set(Environment.XAUTHTOKEN, authToken)
                      //       .set(Environment.XAUTHTOKEN, authToken)
      })
      return next.handle(authReq)
    }else{
      this.logger.debug("auth-interceprtor: no authToken!")
      return next.handle(req)
    }


  }
}
