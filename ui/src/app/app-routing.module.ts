import { NgModule }              from '@angular/core'
import { RouterModule, Routes }  from '@angular/router'

import { AppComponent } from './app.component'
import { LoginComponent } from './pages/login/login.component'
import { UserBetsComponent } from './pages/user-bets/user-bets.component'
import { UserEditComponent } from './pages/user-edit/user-edit.component';
import { SpecialbetPlayerComponent } from './pages/specialbet-player/specialbet-player.component';
import { SpecialbetTeamComponent } from './pages/specialbet-team/specialbet-team.component';
import { UserSpecialbetsComponent } from './pages/user-specialbets/user-specialbets.component';
import { GameEditComponent } from './pages/game-edit/game-edit.component';
import { GameCreateComponent } from './pages/game-create/game-create.component';
import { GamesComponent } from './pages/games/games.component';
import { MailComponent } from './pages/mail/mail.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { StatisticsComponent } from './pages/statistics/statistics.component';
import { RegisterUserComponent } from './pages/register-user/register-user.component';
import { PasswordRequestComponent } from './pages/password-request/password-request.component';
import { RulesComponent } from './pages/rules/rules.component';
import { GameBetsComponent } from './pages/game-bets/game-bets.component';
import { UsersComponent } from './pages/users/users.component';
import { PasswordComponent } from './pages/password/password.component';


const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'logout', component: LoginComponent },
  { path: 'admin/register', component: RegisterUserComponent},
  { path: 'admin/mail', component: MailComponent},
  { path: 'admin/settings', component: SettingsComponent},
  { path: 'user/:username/bets', component: UserBetsComponent},
  { path: 'user/:username/edit', component: UserEditComponent},
  { path: 'user/:username/special/player/:id', component: SpecialbetPlayerComponent},
  { path: 'user/:username/special/team/:id', component: SpecialbetTeamComponent},
  { path: 'user/:username/special', component: UserSpecialbetsComponent},
  { path: 'user/:username', component: UserBetsComponent}, //??
  { path: 'game/:gamenr/edit', component: GameEditComponent},
  { path: 'game/:gamenr/create', component: GameCreateComponent},
  { path: 'game/:gamenr/bets', component: GameBetsComponent},
  { path: 'game/:gamenr', component: GameBetsComponent},
  { path: 'games', component: GamesComponent },
  { path: 'users', component: UsersComponent },
  { path: 'statistics', component: StatisticsComponent},
  { path: 'completeregistration/:token', component: PasswordComponent},
  { path: 'changepassword/:token', component: PasswordComponent},
  { path: 'password', component: PasswordRequestComponent},
  { path: 'rules', component: RulesComponent },
  {
    path: '**',  //TODO: redirect to login if not loggedin and wrong, otherwise users home page
    redirectTo: '/games',
    pathMatch: 'full'
  }
];



@NgModule({
  imports: [
    RouterModule.forRoot(
      routes,
      { enableTracing: false } // <-- debugging purposes only
    )
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {}
