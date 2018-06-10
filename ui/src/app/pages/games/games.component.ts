import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material';
import { Observable } from 'rxjs';
import { GameWithTeams } from '../../model/bet';
import { FilterService } from '../../service/filter.service';
import filter from 'lodash/filter'


@Component({
  selector: 'games',
  templateUrl: './games.component.html',
  styleUrls: ['./games.component.css']
})
export class GamesComponent implements OnInit {
  games$: Observable<GameWithTeams[]>

  constructor(private betterdb: BetterdbService, private snackBar: MatSnackBar,private filterService: FilterService) { }

  filter(games: GameWithTeams[]){
     return filter(games, function(g){ return this.filterService.filterGL(g) })
  }

  ngOnInit() {
    this.games$ = this.betterdb.getGames()
  }

}
