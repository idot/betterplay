import { Component, OnInit } from '@angular/core';
import { Input } from '@angular/core';
import { GameWithTeams } from '../../model/bet';
import { Observable } from 'rxjs';
import { GameWithBetsUsers } from '../../model/user';
import { BetterdbService } from '../../betterdb.service';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { Inject } from '@angular/core';
import forEach from 'lodash/forEach';


  //TODO: add heatmap team1 vs team2
@Component({
  selector: 'game-chart',
  templateUrl: './game-chart.component.html',
  styleUrls: ['./game-chart.component.css']
})
export class GameChartComponent  {


  predictions: any[]

  constructor(public dialogRef: MatDialogRef<GameChartComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) {

    this.predictions = data
    const colors = new Array<string>()
    forEach(this.predictions, function(p){
      colors.push(p.color)
    })
    this.colorScheme.domain = colors

    //TODO: collect colors in data => colorScheme

  }

  view: any[] = [300, 200]

  // options
  showXAxis = true
  showYAxis = true
  gradient = false
  showLegend = false
  showXAxisLabel = false
  xAxisLabel = "blabla"
  showYAxisLabel = false
  yAxisLabel = 'count'



  colorScheme = {
    domain: ['#377eb8']
  }



}
