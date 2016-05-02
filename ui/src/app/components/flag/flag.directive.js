(function() {
  'use strict';

  angular
    .module('ui')
    .directive('flag', flag);

  /** @ngInject */
  function flag() { //<span class="flag-icon flag-icon-gr flag-icon-squared"></span>
      return {
  	     replace: true,
		 restrict: 'E',
	         link: function(scope, element, attrs) {//"attrs.iso"
    	              var flag = '<span class="flag-icon flag-icon-'+attrs.iso+'flag-icon-squared "></span>';
   	              element.append(flag);
                 }
             }
     };	  
      

})();
