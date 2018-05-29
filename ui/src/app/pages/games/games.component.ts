import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material';
import { Observable } from 'rxjs';
import { GameWithTeams } from '../../model/bet';

@Component({
  selector: 'games',
  templateUrl: './games.component.html',
  styleUrls: ['./games.component.css']
})
export class GamesComponent implements OnInit {
  games$: Observable<GameWithTeams[]>

  constructor(private betterdb: BetterdbService, private snackBar: MatSnackBar) { }

  ngOnInit() {
    this.games$ = this.betterdb.getGames()
  }

}
