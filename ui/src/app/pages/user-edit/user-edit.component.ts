import { Component, OnInit } from '@angular/core';
import {FormControl, Validators, FormGroup, FormBuilder, AbstractControl} from '@angular/forms';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';



@Component({
  selector: 'user-edit',
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.css']
})
export class UserEditComponent implements OnInit {

  editForm: FormGroup

  createForm(){
    this.editForm = this.fb.group({
      firstname : ['', [Validators.required]],
      lastname : ['', [Validators.required]],
      passwords: this.fb.group({
                    password: ['', [Validators.required]],
                    confirm_password: ['', [Validators.required]],
                }, {validator: this.passwordsIdentical}),
      email: ['',[Validators.required, Validators.email]]
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


  constructor(private fb: FormBuilder, private userService: UserService, private betterdb: BetterdbService) {
   this.createForm()
 }

  ngOnInit() {
  }

}
