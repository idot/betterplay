(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterPasswordView', betterPasswordView);

  /** @ngInject */
  function betterPasswordView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/password/password.html',
  //    require: ['^form'],
      scope: {
         password1: '=password1',
         password2: '=password2',
         validPassword: '&', 
      }//,
    //  controller: PasswordViewController,
   //   controllerAs: 'vm'//,
      
//      link: function(scope, element, attrs, ctrls) {
//            scope.form = ctrls[0];

//            scope.$watch('age', function() {
//              ngModel.$setViewValue(scope.age);
//            });
//          }
      
      
    };

    return directive;


  }

})();