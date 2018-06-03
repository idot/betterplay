import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { FormGroup } from '@angular/forms';
import { FormBuilder } from '@angular/forms';
import { Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material';
import { ToastComponent } from '../../components/toast/toast.component';

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

  constructor(private snackBar: MatSnackBar, private betterdb: BetterdbService, private fb: FormBuilder) {
      this.createForm()
  }

  ngOnInit() {
      this.recaptchasite = this.betterdb.getSettings().recaptchasite
      console.log(`recaptchasite ${this.recaptchasite}`)
  }


  setResponse(response){
    const details = this.requestForm.value
    const req = {
        email: details['email'],
        response: response
    }
    this.betterdb.changePasswordRequest(req).subscribe( result => {
        this.snackBar.openFromComponent(ToastComponent, { data: { message: result, level: "ok"}})

    })
  }



}
