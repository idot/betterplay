import { Component, OnInit } from '@angular/core';
import { UserService } from '../user/user.service';


@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  username: string = "aa";
  password: string = "";

  constructor(private userService: UserService) { }

  ngOnInit() {
  }

  onSubmit(){
    this.login(this.username, this.password)
  }

  login(username: string, password: string){
      let result = this.userService.login(username, password);
      if(result){
          
      }
  }


}
