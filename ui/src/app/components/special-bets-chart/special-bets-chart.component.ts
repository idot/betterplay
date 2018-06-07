import { Component, OnInit } from '@angular/core';
import { Input } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import groupBy from 'lodash/groupBy';
import map from 'lodash/map';

export var single = [
  {
    "name": "Germany",
    "value": 8940000
  },
  {
    "name": "USA",
    "value": 5000000
  },
  {
    "name": "France",
    "value": 7200000
  }
];


@Component({
  selector: 'special-bets-chart',
  templateUrl: './special-bets-chart.component.html',
  styleUrls: ['./special-bets-chart.component.css']
})
export class SpecialBetsChartComponent implements OnInit {
  @Input() betname


  ngOnInit() {
     this.betterdb.getSpecialBetStats(this.betname).subscribe( data => {
       const template = data.template
       const grouped = groupBy(data.predictions, function(b) {
                return b;
       })
       const bets = map(grouped, function(v, k) {
       const item = k.toString() == "" ? "undecided" : k.toString()
       const arr = { name: item, value: v.length }
                return arr
       })
       const result = { //for D3
              template: template,
              data: [{
                key: template.name,
                values: bets
           }]}
       this.single = bets
       this.xAxisLabel = this.betname
      }
    )
  }

    single: any[]

    view: any[] = [320, 200]

    // options
    showXAxis = true
    showYAxis = true
    gradient = false
    showLegend = false
    showXAxisLabel = true
    xAxisLabel = this.betname
    showYAxisLabel = true
    yAxisLabel = 'count'

    colorScheme = {
      domain: ['#377eb8']
    }

    constructor(private betterdb: BetterdbService) {

    }

    onSelect(event) {

    }
}
