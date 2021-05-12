import { Component, OnInit, ViewEncapsulation  } from '@angular/core';
import { Input } from '@angular/core';
import { GameWithTeams } from '../../model/bet';
import { Observable } from 'rxjs';
import { GameWithBetsUsers } from '../../model/user';
import { BetterdbService, GameStats } from '../../betterdb.service';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Inject } from '@angular/core';
import forEach from 'lodash/forEach';

declare let d3: any;


  //TODO: add heatmap team1 vs team2
@Component({
  selector: 'game-chart',
  templateUrl: './game-chart.component.html',
//styleUrls: ['./game-chart.component.css']
// include original styles
  styleUrls: [
     '../../../../node_modules/nvd3/build/nv.d3.css'
  ],
encapsulation: ViewEncapsulation.None

})
export class GameChartComponent {
  gameStats: GameStats
  bcdata;
  hmdata;
  barchart;
  heatmap;


    constructor(public dialogRef: MatDialogRef<GameChartComponent>,
      @Inject(MAT_DIALOG_DATA) public data: any) {
      this.gameStats = data
      this.barchartOptions()
      this.heatmapOptions()


      this.bcdata = [data.barchart]
      this.hmdata = data.heatmap

    }

    barchartOptions(){
      this.barchart = {
        chart: {
          type: 'discreteBarChart',
          height: 250,
          margin : {
            top: 20,
            right: 20,
            bottom: 50,
            left: 20
          },
          x: function(d){
            return d.label
          },
          y: function(d){return d.value },
          color: function(d){return d.color},
          showValues: false,
          showYAxis: true,
          showXAxis: true,
          valueFormat: function(d){
            return d3.format('0')(d);
          },
          duration: 500,
          xAxis: {
            axisLabel: "                    "+this.gameStats.team1+"        -        "+this.gameStats.team2
          },
          yAxis: {
  //          axisLabel: 'Y Axis',
  //          axisLabelDistance: -10
          }
        }
      }
    }

    heatmapOptions(){
       this.heatmap = {
         chart: {
           type: 'heatMap',
           width: 250,
           height: 250,
           margin : {
             top: 20,
             right: 20,
             bottom: 20,
             left: 20
           },
             x: function(d) { return d.team1 },
             y: function(d) { return d.team2 },
             cellValue: function(d) { return d.count },
             cellAspectRatio: true,
             normalize: false,
             colorRange: ["#f7fbff",  "#deebf7","#c6dbef","#9ecae1",  "#6baed6",  "#4292c6",  "#2171b5",  "#08519c",  "#08306b"],
             showXAxis: true,
             showYAxis: true,
             showLegend: true,
             showGrid: false,
             showCellValues: true,
        //     alignXAxis: 'top',
        //     alignYAxis: 'left',
             cellValueFormat: function(d) { return typeof d === 'number' ? d.toFixed(0) : d },
             cellBorderWidth: 0,
             cellRadius: 0,
             missingDataColor: '#bcbcbc',
             missingDataLabel: '',
             xMeta: false,
             yMeta: false,
             metaOffset: 5,
             xAxis: {
               axisLabel: this.gameStats.team1,
               axisLabelDistance: 10
             },
             yAxis: {
               axisLabel: this.gameStats.team2,
               axisLabelDistance: -10
             }
     	  }
    }
  }




}
