import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { FormGroup } from '@angular/forms';
import { FormBuilder } from '@angular/forms';
import { Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastComponent } from '../../components/toast/toast.component';
import {RecaptchaModule} from 'ng-recaptcha';
import {RecaptchaFormsModule} from 'ng-recaptcha/forms';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'password-request',
  templateUrl: './password-request.component.html',
  styleUrls: ['./password-request.component.css']
})
export class PasswordRequestComponent implements OnInit {

  recaptchasite= ""

  requestForm: FormGroup

  createForm(){
    this.requestForm = this.fb.group({
      recaptcha: ['', [Validators.required]],
      email: ['',[Validators.required, Validators.email]]
    })
  }

  constructor(private snackBar: MatSnackBar, private betterdb: BetterdbService, private fb: FormBuilder, private router: Router) {

  }

  ngOnInit() {
      this.recaptchasite = this.betterdb.getSettings().recaptchasite
      this.createForm()
      //console.log(`recaptchasite ${this.recaptchasite}`)
  }


  submit(){
    const formv = this.requestForm.value
    const req = {
        email: formv['email'],
        response: formv['recaptcha']
    }
    //console.log(`submitting ${req}`)
    this.betterdb.changePasswordRequest(req).pipe(
        catchError( (err) => {
           if(err['error']){
             return of(err['error'])
           } else {
             return of({ error: err.message})
           }
        })
     ).subscribe( result => {
        if(result['error']){
           this.snackBar.openFromComponent(ToastComponent, { data: { message: result['error'], level: "error"}})
        } else {
           this.snackBar.openFromComponent(ToastComponent, { data: { message: result['ok'], level: "ok"}})
        }
    })
  }



}
