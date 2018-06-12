import { Component, OnInit } from '@angular/core';
import { NGXLogger } from 'ngx-logger';
import { ActivatedRoute } from '@angular/router';
import { FormControl } from '@angular/forms';

import range from 'lodash/range';

import { BetterdbService } from '../../betterdb.service';
import { UserService } from '../../service/user.service';
import { BetterTimerService } from '../../better-timer.service';
import { ViewEncapsulation } from '@angular/core';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class SettingsComponent implements OnInit {

  startView = "year" //month
  startDate = new Date()
  date = new Date()
  hours = range(0, 23)
  minutes = range(0, 59)

  df = "MM/dd HH:mm"

  hour = 12
  minute = 0

  constructor(private logger: NGXLogger, private route: ActivatedRoute,
             public betterdb: BetterdbService,  public userService: UserService,
             private timeService: BetterTimerService
            ) { }



  ngOnInit() {
     this.startDate = this.timeService.getTime()
     this.date = this.startDate
  }

  updateDate(){
     this.timeService.updateDate(this.date)
  }

  updateTime(){
     this.timeService.updateTime(this.hour, this.minute)
  }

  resetTime(){
     this.timeService.resetTime()
  }


  mailpassword = ""
  setMailPassword(){
      this.betterdb.setMailPassword(this.mailpassword).subscribe( result =>
          this.logger.log(result)
      )
  }


}
