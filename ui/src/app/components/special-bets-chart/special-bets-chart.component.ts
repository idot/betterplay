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

export var multi = [
  {
    "name": "Germany",
    "series": [
      {
        "name": "2010",
        "value": 7300000
      },
      {
        "name": "2011",
        "value": 8940000
      }
    ]
  },

  {
    "name": "USA",
    "series": [
      {
        "name": "2010",
        "value": 7870000
      },
      {
        "name": "2011",
        "value": 8270000
      }
    ]
  },

  {
    "name": "France",
    "series": [
      {
        "name": "2010",
        "value": 5000002
      },
      {
        "name": "2011",
        "value": 5800000
      }
    ]
  }
];

@Component({
  selector: 'special-bets-chart',
  templateUrl: './special-bets-chart.component.html',
  styleUrls: ['./special-bets-chart.component.css']
})
export class SpecialBetsChartComponent implements OnInit {
  @Input() betname: string
//  constructor() { }

  ngOnInit() {
     this.betterdb.getSpecialBetStats(this.betname).subscribe( data => {
       var template = data.template
       var grouped = groupBy(data.predictions, function(b) {
                return b;
       });
       var bets = map(grouped, function(v, k) {
       var item = k.toString() == "" ? "undecided" : k.toString();
       var arr = { label: item, value: v.length };
                return arr;
       });
       var result = {
              template: template,
              data: [{
                key: template.name,
                values: bets
           }]
      }})
  }
    single: any[]
    multi: any[]

    view: any[] = [700, 400]

    // options
    showXAxis = true
    showYAxis = true
    gradient = false
    showLegend = false
    showXAxisLabel = true
    xAxisLabel = 'Country'
    showYAxisLabel = true
    yAxisLabel = this.betname

    colorScheme = {
      domain: ['#377eb8']
    }

    constructor(private betterdb: BetterdbService) {
      Object.assign(this, { single })
    }

    onSelect(event) {
      console.log(event);
    }
}
