(function() {
  'use strict';

  angular
    .module('ui')
    .run(runBlock);

  /** @ngInject */
  function runBlock($log, Restangular, toastr, _) {
    
    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
    // if(response.status === 403) {
        // refreshAccesstoken().then(function() {
             // Repeat the request and then call the handlers the usual way.
      //       $http(response.config).then(responseHandler, deferred.reject);
    //         // Be aware that no request interceptors are called this way.
    //     });

   //      return false; // error handled
   //  }
         var errors = _.map(response.data, function(value, key){ return key + " " +value; });
	 var errorsum = errors.join("\n");
         toastr.error('error', "application error: "+response.statusText, errorsum);
         $log.error("custom error handler: "+response.status+" "+response.data);
        return true; // error not handled
     });
     $log.debug('runBlock end');
  
    
  }

})();
