import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { NGXLogger } from 'ngx-logger';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { User } from '../../model/user';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  users$: Observable<User[]>
  JSON = JSON

  constructor(private logger: NGXLogger, private route: ActivatedRoute, private betterdb: BetterdbService) { }

  ngOnInit() {
    this.users$ = this.betterdb.getUsers()
  }

}
