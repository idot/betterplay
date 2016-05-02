(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterFlag', betterFlag);

  /** @ngInject */
  function betterFlag() { //<span class="flag-icon flag-icon-gr flag-icon-squared"></span>
      return {
                 replace: true,
		 restrict: 'E',
                 scope:  {
                     iso: '='
                 },
                 template: "<span class='flag-icon flag-icon-{{iso}} flag-icon-squared'></span>" 
             }
     }	  
      

})();
