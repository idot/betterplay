import { BrowserModule } from '@angular/platform-browser'
import { NgModule } from '@angular/core'

import { HttpClientModule } from '@angular/common/http'
import { FormsModule }   from '@angular/forms'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'

import {MatIconModule} from '@angular/material/icon'
import {DomSanitizer} from '@angular/platform-browser'
import {MatIconRegistry, MatButtonModule, MatCheckboxModule} from '@angular/material'


import { LoggerModule, NgxLoggerLevel } from 'ngx-logger'

import { httpInterceptorProviders } from './http-interceptors'
import { AppRoutingModule } from './app-routing.module'

import { AppComponent } from './app.component'
import { LoginComponent } from './pages/login/login.component'
import { UserBetsComponent } from './pages/user-bets/user-bets.component'

import { BetterdbService } from 'src/app/betterdb.service'
import { UserService } from './user/user.service'
import { BetViewComponent } from './components/bet-view/bet-view.component'
import { GameViewComponent } from './components/game-view/game-view.component'
import { UserViewComponent } from './components/user-view/user-view.component'
import { FilterViewComponent } from './components/filter-view/filter-view.component'
import { MenuComponent } from './components/menu/menu.component'



//enable server side logging with serverLogginUrl
//LoggerModule.forRoot({serverLoggingUrl: '/api/logs', level: NgxLoggerLevel.DEBUG, serverLogLevel: NgxLoggerLevel.ERROR}),

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    UserBetsComponent,
    BetViewComponent,
    GameViewComponent,
    UserViewComponent,
    FilterViewComponent,
    MenuComponent
  ],
  imports: [
    BrowserModule,
    LoggerModule.forRoot({level: NgxLoggerLevel.DEBUG}),
    FormsModule,
    MatIconModule, MatButtonModule, MatCheckboxModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserAnimationsModule
  ],
  providers: [httpInterceptorProviders, UserService, BetterdbService],
  bootstrap: [AppComponent]
})
export class AppModule {

  constructor(private iconRegistry: MatIconRegistry, private sanitizer: DomSanitizer) {
    iconRegistry.addSvgIcon(
        'do_not_disturb',
        sanitizer.bypassSecurityTrustResourceUrl('assets/img/examples/thumbup-icon.svg'));
        iconRegistry.addSvgIcon(
              'thumbs-up',
              sanitizer.bypassSecurityTrustResourceUrl('assets/img/examples/thumbup-icon.svg'));

  }



}
