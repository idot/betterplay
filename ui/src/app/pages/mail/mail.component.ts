import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastComponent } from '../../components/toast/toast.component';
import { FormGroup } from '@angular/forms';
import { FormBuilder } from '@angular/forms';
import { Validators } from '@angular/forms';

@Component({
  selector: 'mail',
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.css']
})
export class MailComponent implements OnInit {

  constructor(private fb: FormBuilder, private betterdb: BetterdbService, private snackBar: MatSnackBar) {
     this.createForm()
  }

  mailForm: FormGroup

  createForm(){
    this.mailForm = this.fb.group({
      subject : ['', [Validators.required]],
      body: ['', [Validators.required]]
    })
  }

  ngOnInit() {
  }

  sendUnsentMail = function(){
      if(! confirm("Are you sure to send the current unsent mails?")){
        return
      }
      this.betterdb.sendUnsentMail().subscribe( result => {
          this.snackBar.openFromComponent(ToastComponent, { data: { message: "sent unsert mail", level: "ok"}})
      })
   }

  createMail(){
    if(! confirm("Are you sure to save this mail to the database?")){
      return
    }
    const message = this.mailForm.value
    this.createForm()
    this.betterdb.createMail(message).subscribe( data => {
        this.snackBar.openFromComponent(ToastComponent, { data: { message: "saved mail in database", level: "ok"}})
    })

  }

}
