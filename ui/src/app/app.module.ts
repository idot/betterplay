
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule }   from '@angular/forms';


import { AppComponent } from './app.component';
import { AppService } from './app.service';
import { HttpClientModule } from '@angular/common/http';
import { RouteExampleComponent } from './route-example/route-example.component';
import { LoginComponent } from './login/login.component';

import { UserService} from './user/user.service';
import { UserBetsComponent } from './user-bets/user-bets.component'

const routes: Routes = [
  {
    path: 'scala',
    component: RouteExampleComponent,
    data: {technology: 'Scala'}
  },
  {
    path: 'play',
    component: RouteExampleComponent,
    data: {technology: 'Play'}
  },
  {
    path: '/:username/bets',
    component: UserBetsComponent
  },
  {
    path: 'angular',
    component: RouteExampleComponent,
    data: {technology: 'Angular'}
  },
  {
    path: '**',
    redirectTo: '/play',
    pathMatch: 'full'
  }
];

@NgModule({
  declarations: [
    AppComponent,
    RouteExampleComponent,
    LoginComponent,
    UserBetsComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    RouterModule.forRoot(routes)
  ],
  providers: [AppService, UserService],
  bootstrap: [AppComponent]
})
export class AppModule {
}
