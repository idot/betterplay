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
	   filterChanged: '&',      // Pass a reference to the method 
      },
      controller: FilterViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function FilterViewController($log, Restangular, toastr, betterSettings, userService, $scope, _) {
        var vm = this;
        vm.userService = userService;
       
        vm.gameFilter = ['all', 'open', 'closed'];
        vm.levelFilter = ['all', 'group', 'last16', 'quarter', 'semi', 'third', 'final'];
        vm.betFilter = ['all','set', 'not set'];
        
        vm.filterChanged = function(){
            $scope.filterChanged();
            userService.saveFilter();  
        };    
        
    
        vm.resetFilter = function(){
            var filter = {
                bet : "all",
                level : "all",
                game: "all"
            };

            vm.userService.filter = filter;
            vm.filterChanged();
        };
        
    };
  }

})();