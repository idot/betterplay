import { Component, OnInit } from '@angular/core';
import { Input } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import groupBy from 'lodash/groupBy';
import map from 'lodash/map';




@Component({
  selector: 'special-bets-chart',
  templateUrl: './special-bets-chart.component.html',
  styleUrls: ['./special-bets-chart.component.css']
})
export class SpecialBetsChartComponent implements OnInit {
  @Input() betname

  bcdata: any
  barchart: any

  ngOnInit() {
     this.barchartOptions()
     this.getData()
  }

  getData(){
    this.betterdb.getSpecialBetStats(this.betname).subscribe( data => {
      const template = data.template
      const grouped = groupBy(data.predictions, function(b) {
               return b;
      })
      const bets = map(grouped, function(v, k) {
          const item = k.toString() == "" ? "undecided" : k.toString()
          const arr = { label: item, value: v.length }
          return arr
      })
      const result = { //for D3
             template: template,
             data: [{
               key: template.name,
               values: bets
      }]}
      this.bcdata = result.data
     }
    )
  }

  barchartOptions(){
    this.barchart = {
      chart: {
        type: 'discreteBarChart',
        height: 320,
        margin : {
          top: 10,
          right: 20,
          bottom: 100,
          left: 30
        },
        x: function(d){
          return d.label
        },
        y: function(d){return d.value },
        color: function(d){ return "#80b1d3" },
        showValues: false,
        showYAxis: true,
        showXAxis: true,
        valueFormat: function(d){
          return d3.format('0')(d);
        },
        duration: 500,
        xAxis: {
        //  axisLabel: this.betname,
          rotateLabels: -90
        },
        yAxis: {
//          axisLabel: 'Y Axis',
//          axisLabelDistance: -10
        }
      }
    }
  }


    constructor(private betterdb: BetterdbService) {

    }

    onSelect(event) {

    }
}
