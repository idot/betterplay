import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { HttpClientModule } from '@angular/common/http';
import { FormsModule }   from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'

import { LoggerModule, NgxLoggerLevel } from 'ngx-logger';

import { httpInterceptorProviders } from './http-interceptors';
import { AppRoutingModule } from './app-routing.module';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { UserBetsComponent } from './user-bets/user-bets.component';

import { BetterdbService } from 'src/app/betterdb.service';
import { UserService } from './user/user.service';
import { BetViewComponent } from './components/bet-view/bet-view.component';



//enable server side logging with serverLogginUrl
//LoggerModule.forRoot({serverLoggingUrl: '/api/logs', level: NgxLoggerLevel.DEBUG, serverLogLevel: NgxLoggerLevel.ERROR}),

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    UserBetsComponent,
    BetViewComponent
  ],
  imports: [
    BrowserModule,
    LoggerModule.forRoot({level: NgxLoggerLevel.DEBUG}),
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserAnimationsModule
  ],
  providers: [httpInterceptorProviders, UserService, BetterdbService],
  bootstrap: [AppComponent]
})
export class AppModule { }
