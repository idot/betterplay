import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { NGXLogger } from 'ngx-logger';

import { UserService } from '../user/user.service';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  username: string = "aa";
  password: string = "";

  constructor(private logger: NGXLogger, private router: Router, private userService: UserService) { }

  ngOnInit() {
  }

  onSubmit(){
    this.login(this.username, this.password)
  }

  login(username: string, password: string){
      const result = this.userService.login(username, password);
      this.logger.debug(`fetched user at login: ${username}`, result);
      if(result){
        //  this.router.navigate([`/user/:username/bets`, { username: username }]);
          this.router.navigate([`/user/${username}/bets`]);
      }
  }


}
