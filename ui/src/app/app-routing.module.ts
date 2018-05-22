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


const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'loout', component: LoginComponent },
  { path: 'user/register', component: RegisterUserComponent},
  { path: 'user/:username/bets', component: UserBetsComponent},
  { path: 'user/:username/edit', component: UserEditComponent},
  { path: 'user/:username/special/player/:id', component: SpecialbetPlayerComponent},
  { path: 'user/:username/special/team/:id', component: SpecialbetTeamComponent},
  { path: 'user/:username/special', component: UserSpecialbetsComponent},
  { path: 'user/:username', component: UserBetsComponent}, //??
  { path: 'game/:gamenr/edit', component: GameEditComponent},
  { path: 'game/:gamenr/create', component: GameCreateComponent},
  { path: 'game/:gamenr/bets', component: GamesComponent},
  { path: 'game/:gamenr', component: GamesComponent}, //??
  { path: 'mail', component: MailComponent},
  { path: 'settings', component: SettingsComponent},
  { path: 'statistics', component: StatisticsComponent},
  { path: 'password', component: PasswordRequestComponent},
  {
    path: '**',  //TODO: redirect to login if not loggedin and wrong, otherwise users home page
    redirectTo: '/',
    pathMatch: 'full'
  }
];



@NgModule({
  imports: [
    RouterModule.forRoot(
      routes,
      { enableTracing: true } // <-- debugging purposes only
    )
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule {}
