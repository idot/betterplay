import { Component, OnInit } from '@angular/core';
import {FormControl, Validators, FormGroup, FormBuilder, AbstractControl} from '@angular/forms';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';

@Component({
  selector: 'register-user',
  templateUrl: './register-user.component.html',
  styleUrls: ['./register-user.component.css']
})
export class RegisterUserComponent implements OnInit {
  //email = new FormControl('', [Validators.required, Validators.email])
//  username = new FormControl('', [Validators.required])
  registerForm: FormGroup



  createForm(){
    this.registerForm = this.fb.group({
      username : ['', [Validators.required]],
      firstname: ['', [Validators.required]],
      lastname: ['', [Validators.required]],
      email: ['',[Validators.required, Validators.email]]
    })
  }




  getErrorMessage() {
    // return this.email.hasError('required') ? 'You must enter a value' :
  //          this.email.hasError('email') ? 'Not a valid email' :
  //          ''
   }

  constructor(private fb: FormBuilder, private userService: UserService, private betterdb: BetterdbService) {
     this.createForm()
  }

  ngOnInit() {
  }

  register(){
    const user = this.userService.getUser()
    if(user && user.isAdmin){
      const details = this.registerForm.value
      this.betterdb.createUser(details)
      console.log(details.username)
      //TODO get return value
    } else {
      //TODO: error
    }
  }

}
