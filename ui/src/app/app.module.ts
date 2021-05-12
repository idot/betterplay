import { BrowserModule } from '@angular/platform-browser'
import { NgModule } from '@angular/core'

import { HttpClientModule } from '@angular/common/http'
import { FormsModule }   from '@angular/forms'
import { FlexLayoutModule } from '@angular/flex-layout';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'

import {MatIconModule} from '@angular/material/icon'
import {DomSanitizer} from '@angular/platform-browser'
import {MatMenuModule} from '@angular/material/menu';
import {MatSidenavModule} from '@angular/material/sidenav';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatNativeDateModule, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatIconRegistry } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule, MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';
import {MatTooltipModule} from '@angular/material/tooltip'
import {MatTableModule} from '@angular/material/table';
import {MatSortModule} from '@angular/material/sort';
import {MatDialogModule} from '@angular/material/dialog';

//import { MaterialFileInputModule } from 'ngx-material-file-input'

import { LoggerModule, NgxLoggerLevel } from 'ngx-logger'

import { httpInterceptorProviders } from './http-interceptors'
import { AppRoutingModule } from './app-routing.module'

import { RecaptchaModule } from 'ng-recaptcha';
import { RecaptchaFormsModule } from 'ng-recaptcha/forms';

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
import { ToastComponent } from './components/toast/toast.component';
import { SpecialBetsChartComponent } from './components/special-bets-chart/special-bets-chart.component';
import { PasswordComponent } from './pages/password/password.component';
import { APP_INITIALIZER } from '@angular/core';
import { Pipe } from '@angular/core';
import { EvenoddPipe } from './pipes/evenodd.pipe';
import { APP_BASE_HREF } from '@angular/common';
import { FilterService } from './service/filter.service';
import { GameChartComponent } from './components/game-chart/game-chart.component';
import { NvD3Module } from 'ng2-nvd3';

// d3 and nvd3 should be included somewhere
import 'd3';
import 'nvd3';


/**
* initialise betterdb with settings at startup APP_INITIALIZER
*/
export function loadSettings(betterdb: BetterdbService){
    return () => betterdb.loadSettings()
}


export function reauthenticate(userService: UserService){
    return () => userService.reauthenticate()
}

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
    UsersComponent,
    ToastComponent,
    SpecialBetsChartComponent,
    PasswordComponent,
    EvenoddPipe,
    GameChartComponent
  ],
  imports: [
    BrowserModule, NvD3Module,
    LoggerModule.forRoot({level: NgxLoggerLevel.DEBUG}),
    FormsModule, ReactiveFormsModule, FlexLayoutModule,
    MatIconModule, MatButtonModule, MatCheckboxModule, MatBadgeModule, MatInputModule, MatSelectModule, MatSnackBarModule,
    MatTableModule, MatSortModule, MatProgressSpinnerModule, MatDatepickerModule, MatNativeDateModule,
    MatTooltipModule, MatMenuModule, MatSidenavModule,
    HttpClientModule, MatDialogModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    //MaterialFileInputModule,
    RecaptchaModule.forRoot(), RecaptchaFormsModule
  ],
  entryComponents: [
      ToastComponent, GameChartComponent
  ],
  providers: [httpInterceptorProviders, BetterTimerService, FilterService,
       {provide: MAT_SNACK_BAR_DEFAULT_OPTIONS, useValue: {duration: 2500, panelClass: ['toast-background'] }},
       {provide: APP_INITIALIZER, deps: [UserService], multi: true, useFactory: reauthenticate },
       {provide: APP_INITIALIZER, deps: [BetterdbService], multi: true, useFactory: loadSettings },
       {provide: MAT_DATE_LOCALE, useValue: 'en-GB'},
       UserService, BetService,
  ],
  bootstrap: [AppComponent]
})


export class AppModule {

  constructor(private iconRegistry: MatIconRegistry, private sanitizer: DomSanitizer) {


  }



}
