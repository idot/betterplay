import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { UserService } from '../../service/user.service';

import * as FileSaver from 'file-saver';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastComponent } from '../../components/toast/toast.component';


@Component({
  selector: 'statistics',
  templateUrl: './statistics.component.html',
  styleUrls: ['./statistics.component.css']
})
export class StatisticsComponent implements OnInit {

  getExcel(){
    const loggedin = this.userService.isLoggedIn()
    return this.betterdb.getExcel(loggedin).subscribe(data  => {
        //console.log(`downloading data`)
        const content = data.headers.get("content-disposition")
        const blob = data.body
        if(content && blob){
            const filename = content.split(" ")[1].split("=")[1]
            FileSaver.saveAs(blob, filename, true)
        } else if(blob) {
            this.snackBar.openFromComponent(ToastComponent, { data: { message: "wrong filename, will not verify", level: "warning"}})
              FileSaver.saveAs(blob, "bets.xlsx", true)
        } else {
            this.snackBar.openFromComponent(ToastComponent, { data: { message: "could not download excel", level: "error"}})
        }
    },
    error => console.log("Error downloading the file."),
    ()  => console.log('Completed file download.'))
  }


  


  constructor(private betterdb: BetterdbService, private userService: UserService, private snackBar: MatSnackBar) { }

  ngOnInit() {
  }

}
