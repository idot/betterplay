(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterFilterView', betterFilterView);

  /** @ngInject */
  function betterFilterView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/filter/filter.html',
      scope: {
          //userFilter: '=userFilter'
         // game: '=game'
	   filterChanged: '&',      // Pass a reference to the method 
      },
      controller: FilterViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function FilterViewController($log, Restangular, toastr, betterSettings, userService, $scope, _) {
        var vm = this;
        vm.filter = userService.filter;
       
        vm.gameFilter = ['all', 'open', 'closed'];
        vm.levelFilter = ['all', 'group', 'last16', 'quarter', 'semi', 'third', 'final'];
        vm.betFilter = ['all','set', 'not set'];
        
        vm.filterChanged = function(){
            $scope.filterChanged();
        };    
        
    };
  }

})();