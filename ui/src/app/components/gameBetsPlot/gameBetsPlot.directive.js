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
                     gwt: '='
                 },
                 templateUrl: "app/components/gameBetsPlot/gameBetsPlot.html",
                 controller: GameBetsPlotController,
                 controllerAs: 'vm'  
            };
             
            return directive;
            

           /** @ngInject */
           function GameBetsPlotController($log, gameBetStats, $timeout, $scope, _) {
                 var vm = this;
                 vm.plotData = null; 
                 
                 vm.gwt = $scope.gwt;
                   
                 vm.plotOptions = {
                       chart: {
                         type: 'multiBarHorizontalChart',
                         showControls: false,
                         stacked: true,
                         height: 400,
                         width: 600,
                         margin : {
                             top: 15,
                             right: 25,
                             bottom: 50,
                             left: 20
                         },
                         x: function(d){return d.label;},
                         y: function(d){return d.value;},
                         showValues: false,
                         duration: 500,
                         xAxis: {
                             //axisLabel: 'prediction'
                         },
                         yAxis: {
                             axisLabel: 'count',
                             axisLabelDistance: -5,
                             tickFormat: function(d){
                                return d3.format('.1')(Math.abs(d));
                            }
                         }
                     }
                 };

                var loadData = function(){
                     gameBetStats.getStats($scope.gwt.game.id, $scope.gwt.team1.name,  $scope.gwt.team2.name).then(function(tb) {
                          var dMax = tb.max;
                          vm.plotOptions.chart.forceY = [-dMax,+dMax];
                          vm.plotData = tb.dat;
                     });
                 };
                 
                 loadData();

               //        $timeout(loadData, 1000);
             }
             
     }	  
      

})(); 