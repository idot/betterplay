import { Component, OnInit } from '@angular/core';
import { Inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA } from '@angular/material';
import { MatSnackBarRef } from '@angular/material';


//must probably style the surroundings with app.component.css
//this.snackBar.openFromComponent(ToastComponent, { data: "blabla" } )
//the css of parent element is removed by app.component.css and the injection of toast-background

export interface Toast {
  message: string
  level: string
}

@Component({
  selector: 'toast',
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent implements OnInit {

  constructor(private snackBarRef: MatSnackBarRef<ToastComponent>, @Inject(MAT_SNACK_BAR_DATA) public data: Toast) { }

  ngOnInit() {
  }

  close(){
    this.snackBarRef.dismiss();
  }
}
