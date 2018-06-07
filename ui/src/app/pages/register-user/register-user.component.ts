import { Component, OnInit } from '@angular/core';
import {FormControl, Validators, FormGroup, FormBuilder, AbstractControl} from '@angular/forms';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material';
import { ToastComponent } from '../../components/toast/toast.component';
import { Router } from '@angular/router';

import { ErrorMessage } from '../../model/environment';

@Component({
  selector: 'register-user',
  templateUrl: './register-user.component.html',
  styleUrls: ['./register-user.component.css']
})
export class RegisterUserComponent implements OnInit {

  registerForm: FormGroup



  createForm(){
    this.registerForm = this.fb.group({
      username : ['', [Validators.required, Validators.pattern("^[a-z0-9_-]{4,20}$")]],
      firstname: ['', [Validators.required, Validators.pattern("^[a-zA-Z]{3,30}$")]],
      lastname: ['', [Validators.required, Validators.pattern("^[a-zA-Z]{3,30}$")]],
      email: ['',[Validators.required, Validators.email]]
    })
  }



  constructor(private fb: FormBuilder, private userService: UserService, private betterdb: BetterdbService, private snackBar: MatSnackBar, private router: Router) {
     this.createForm()
  }

  ngOnInit() {
  }

  register(){
    const user = this.userService.getUser()
    if(user && user.isAdmin){
      const details = this.registerForm.value
      const result = this.betterdb.createUser(details).subscribe( result => {
          if(result['ok']){
              this.snackBar.openFromComponent(ToastComponent, { data: { message: result['ok'], level: "ok"}})
              this.router.navigate([`/users`])
          } else {
              const message = result['type'] == 'not unique' ? "duplicate username or e-mail" : result['error']
              this.snackBar.openFromComponent(ToastComponent, { data: { message: message, level: "error"}})
          }
      })
    } else {
        this.snackBar.openFromComponent(ToastComponent, { data: { message: "user not logged in or not admin", level: "error"}})
        this.router.navigate([`/login`])
    }
  }

}
