import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material';
import { ToastComponent } from '../../components/toast/toast.component';

@Component({
  selector: 'mail',
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.css']
})
export class MailComponent implements OnInit {

  constructor(private betterdb: BetterdbService, private snackBar: MatSnackBar) { }

  subject = ""
  body = ""

  ngOnInit() {
  }

  sendUnsetMail = function(){
      this.betterdb.sendUnsetMail().pipe( result => {
          this.snackBar.openFromComponent(ToastComponent, { data: { message: "sent unsert mail", level: "ok"}})
      })
   }

  createMail(){
    if(this.subject.trim()  == "" || this.body.trim() == ""){
        this.snackBar.openFromComponent(ToastComponent, { data: { message: "please specify subject and body", level: "error"}})
        return
    }
    const message = {
        subject: this.subject,
        body:this.body
    }
    this.betterdb.createMail(message).pipe( data => {
        this.snackBar.openFromComponent(ToastComponent, { data: { message: "saved mail in database", level: "ok"}})
        this.subject = ""
        this.body = ""
    })

  }

}
