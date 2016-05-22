(function() {
  'use strict';

  angular
    .module('ui')
    .directive('gameBetsPlot', gameBetsPlot);

  /** @ngInject */
  function gameBetsPlot() { 
           var directive = {
                 replace: true,
		 restrict: 'E',
                 scope:  {
                     id: '='
                 },
                 templateUrl: "app/components/gameBetsPlot/gameBetsPlot.html",
                 controller: GameBetsPlotController,
                 controllerAs: 'vm'  
            };
             
            return directive;
            

           /** @ngInject */
           function GameBetsPlotController($log, gameBetStats, $timeout, $scope, _) {
                 var vm = this;
                 vm.plotData = {};
                 
     //            vm.gwt = $scope.gwt;
                 
                 $log.error($scope.id);
                 
                 vm.plotOptions = {
                       chart: {
                         type: 'multiBarHorizontalChart',
                         showControls: false,
                         stacked: true,
                         height: 400,
                         width: 600,
                         forceY: [-30,30],
                         margin : {
                             top: 10,
                             right: 10,
                             bottom: 200,
                             left: 55
                         },
                         x: function(d){return d.label;},
                         y: function(d){return d.value;},
                         showValues: false,
                 //        valueFormat: function(d){
                //             return d3.format('.1')(d);
                //         },
                         duration: 500,
                         xAxis: {
                             rotateLabels: -90
             //                axisLabel: 'X Axis'
                         },
                         yAxis: {
                             axisLabel: 'count',
                             axisLabelDistance: -5,
                             tickFormat: function(d){
                                return d3.format(',.2f')(Math.abs(d));
                            }
                         }
                     }
                 };

                var loadData = function(){
                     gameBetStats.getStats($scope.id, "$scope.gwt.team1.name",  "$scope.gwt.team2.name").then(function(tb) {
                         vm.plotData = tb.data;
                         var dMax = tb.max;
                         vm.plotOptions.chart.forceY = [-dMax,+dMax];
                     });
                 };
                 
                 loadData();

               //        $timeout(loadData, 1000);
             }
             
     }	  
      

})(); 