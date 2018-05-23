import { BrowserModule } from '@angular/platform-browser'
import { NgModule } from '@angular/core'

import { HttpClientModule } from '@angular/common/http'
import { FormsModule }   from '@angular/forms'
import { FlexLayoutModule } from '@angular/flex-layout';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'

import {MatIconModule} from '@angular/material/icon'
import {DomSanitizer} from '@angular/platform-browser'
import {MatIconRegistry, MatButtonModule, MatCheckboxModule, MatBadgeModule, MatInputModule, MatSelectModule} from '@angular/material'
import {MatTableModule} from '@angular/material/table';
import {MatSortModule} from '@angular/material/sort';

import { LoggerModule, NgxLoggerLevel } from 'ngx-logger'

import { httpInterceptorProviders } from './http-interceptors'
import { AppRoutingModule } from './app-routing.module'

import { AppComponent } from './app.component'
import { LoginComponent } from './pages/login/login.component'
import { UserBetsComponent } from './pages/user-bets/user-bets.component'

import { BetterdbService } from 'src/app/betterdb.service'
import { UserService } from './service/user.service'
import { BetService } from './service/bet.service'
import { BetterTimerService } from './better-timer.service';

import { BetViewComponent } from './components/bet-view/bet-view.component'
import { GameViewComponent } from './components/game-view/game-view.component'
import { UserViewComponent } from './components/user-view/user-view.component'
import { FilterViewComponent } from './components/filter-view/filter-view.component'
import { MenuComponent } from './components/menu/menu.component';
import { MenubarComponent } from './components/menubar/menubar.component';
import { UserEditComponent } from './pages/user-edit/user-edit.component';
import { SpecialbetPlayerComponent } from './pages/specialbet-player/specialbet-player.component';
import { SpecialbetTeamComponent } from './pages/specialbet-team/specialbet-team.component';
import { UserSpecialbetsComponent } from './pages/user-specialbets/user-specialbets.component';
import { GamesComponent } from './pages/games/games.component';
import { GameBetsComponent } from './pages/game-bets/game-bets.component';
import { GameCreateComponent } from './pages/game-create/game-create.component';
import { GameEditComponent } from './pages/game-edit/game-edit.component';
import { MailComponent } from './pages/mail/mail.component';
import { PasswordRequestComponent } from './pages/password-request/password-request.component';
import { RegisterUserComponent } from './pages/register-user/register-user.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { StatisticsComponent } from './pages/statistics/statistics.component';
import { RulesComponent } from './pages/rules/rules.component'
import { ReactiveFormsModule } from '@angular/forms';
import { UsersComponent } from './pages/users/users.component';



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
    MenuComponent,
    MenubarComponent,
    UserEditComponent,
    SpecialbetPlayerComponent,
    SpecialbetTeamComponent,
    UserSpecialbetsComponent,
    GamesComponent,
    GameBetsComponent,
    GameCreateComponent,
    GameEditComponent,
    MailComponent,
    PasswordRequestComponent,
    RegisterUserComponent,
    SettingsComponent,
    StatisticsComponent,
    RulesComponent,
    UsersComponent
  ],
  imports: [
    BrowserModule,
    LoggerModule.forRoot({level: NgxLoggerLevel.DEBUG}),
    FormsModule, ReactiveFormsModule, FlexLayoutModule,
    MatIconModule, MatButtonModule, MatCheckboxModule, MatBadgeModule, MatInputModule, MatSelectModule,
    MatTableModule, MatSortModule,
    HttpClientModule,
    AppRoutingModule,
    BrowserAnimationsModule
  ],
  providers: [httpInterceptorProviders, UserService, BetterdbService, BetService, BetterTimerService],
  bootstrap: [AppComponent]
})
export class AppModule {

  constructor(private iconRegistry: MatIconRegistry, private sanitizer: DomSanitizer) {

  }



}
