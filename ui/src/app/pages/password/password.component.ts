import { Component, OnInit } from '@angular/core';
import {FormControl, Validators, FormGroup, FormBuilder, AbstractControl} from '@angular/forms';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { ActivatedRoute } from '@angular/router';
import { Route } from '@angular/compiler/src/core';
import { switchMap, catchError } from 'rxjs/operators';
import { ParamMap } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastComponent } from '../../components/toast/toast.component';
import { NGXLogger } from 'ngx-logger';
import { of } from 'rxjs';

import { Router } from '@angular/router';



@Component({
  selector: 'password',
  templateUrl: './password.component.html',
  styleUrls: ['./password.component.css']
})
export class PasswordComponent implements OnInit {

  editForm: FormGroup
  token = ""

  createForm(){
    this.editForm = this.fb.group({
      passwords: this.fb.group({
                    password: ['', [Validators.required,Validators.pattern("^[ \\S]{4,20}$")]],
                    confirm_password: ['', [Validators.required]],
                }, {validator: this.passwordsIdentical})
    })
  }

  passwordsIdentical(control: AbstractControl): { invalid: boolean } | null {
    const p1 = control.get('password')
    const p2 = control.get('confirm_password')
    if(p1 && p2){
       if(p1.value === p2.value){
          return null
       } else {
          return { invalid: true }
       }
    } else {
      return { invalid: true }
    }
 }


 constructor(private logger: NGXLogger, private snackBar: MatSnackBar, private fb: FormBuilder, private userService: UserService, private betterdb: BetterdbService, private route: ActivatedRoute, private router: Router) {
   this.createForm()
 }

  ngOnInit() {
    this.route.paramMap.pipe(
       switchMap((params: ParamMap) => {
          const token = params.get('token')
          if(token){
             this.logger.log(`registering user with token ${token}`)
             this.token = token
          } else {
             this.snackBar.openFromComponent(ToastComponent, { data: { message: "could not parse token", level: "error"}})
          }
          return of("")
      })
    ).subscribe()
  }

  submit(){
    const complete = this.router.url.startsWith("/complete")

    const pw = this.editForm.value['passwords']['password']
    if(pw){
        const pt = {  password: pw,
                      token: this.token
                   }

        this.betterdb.setTokenPassword(pt).pipe(
          catchError( (err) => {
             return of({ error: err.message})
          })
        ).subscribe( result => {
          if(result['error']){
            this.snackBar.openFromComponent(ToastComponent, { data: { message: `could not complete password\n ${result['error']}`, level: "error"}})
          } else {
             this.userService.setUserData(result)
             const user = this.userService.getUser()
             if(user){
                 if(complete){
                   this.snackBar.openFromComponent(ToastComponent, { data: { message: `welcome ${user.username} and good luck!!`, level: "ok"}})
                   this.router.navigate([`/user/${user.username}/special`])
                 } else {
                   this.snackBar.openFromComponent(ToastComponent, { data: { message: `you changed your password`, level: "ok"}})
                   this.router.navigate([`/user/${user.username}/bets`])
                 }
             } else {
                 const message = complete ? "could not complete registration" : "could not change password"
                 this.snackBar.openFromComponent(ToastComponent, { data: { message: message, level: "error"}})
             }
        }})
    }
  }

}
