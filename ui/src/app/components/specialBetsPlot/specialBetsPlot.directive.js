(function() {
  'use strict';

  angular
    .module('ui')
    .directive('specialBetsPlot', specialBetsPlot);

  /** @ngInject */
  function specialBetsPlot() { 
      var directive = {
                 replace: true,
		 restrict: 'E',
                 scope:  {
                     betname: '='
                 },
                 templateUrl: "app/components/specialBetsPlot/specialBetsPlot.html",
                 controller: SpecialBetsPlotController,
                 controllerAs: 'vm'  
             };
             
             return directive;
            

              /** @ngInject */
             function SpecialBetsPlotController($log, specialBetStats, $scope, _) {
                 var vm = this;
                 vm.plotData = {};
                 vm.plotOptions = {
                       chart: {
                         type: 'discreteBarChart',
                         height: 400,
                         width: 600,
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
                             axisLabel: 'Count',
                             axisLabelDistance: -5
                         }
                     }
                 };

         
                 specialBetStats.getStats($scope.betname).then(function(tb) {
                     vm.template = tb.template;
                     vm.plotData = tb.data;
                 });
                 
                 
             }
             
     }	  
      

})();