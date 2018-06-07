import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { NGXLogger } from 'ngx-logger';

import { UserService } from '../../service/user.service';
import { MatSnackBar } from '@angular/material';
import { ToastComponent } from '../../components/toast/toast.component';
import { ActivatedRoute } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { UrlSegment } from '@angular/router';


@Component({
  selector: 'login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  username: string = "admin";
  password: string = "admin";

  constructor(private logger: NGXLogger,  private router: Router, private snackBar: MatSnackBar, private userService: UserService) { }

  ngOnInit() {
  }

  onSubmit(){
    this.login(this.username, this.password)
  }

  login(username: string, password: string){
      this.userService.login(username, password).subscribe( data => {
          if(data['error']){
             this.snackBar.openFromComponent(ToastComponent, { data: { message: "could not log you in", level: "error"}})
          } else {
             this.userService.setUserData(data)
             const user = this.userService.getUser()
             if(user && user.hadInstructions){
                  this.router.navigate([`/user/${username}/bets`])
             } else {
                  this.snackBar.openFromComponent(ToastComponent, { data: { message: "please set the special bets", level: "warning"}})
                  this.router.navigate([`/user/${username}/special`])
             }
          }
      }
    )
  }


}
