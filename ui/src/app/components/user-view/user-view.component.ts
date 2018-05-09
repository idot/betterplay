import { Component, OnInit, Input } from '@angular/core';

import { BetterdbService } from '../../betterdb.service';
import { BetterTimerService } from '../../better-timer.service';

import { User } from '../../model/user';

@Component({
  selector: 'user-view',
  templateUrl: './user-view.component.html',
  styleUrls: ['./user-view.component.css']
})
export class UserViewComponent implements OnInit {
  @Input() user: User
  @Input() full: Boolean 

  constructor(private betterdb: BetterdbService, private timerService: BetterTimerService) {}


  badgecolour(): string {
      return this.betterdb.badgecolour(this.user.rank)
  }

  ngOnInit() {
  }

}
