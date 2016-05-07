(function() {
  'use strict';

  angular
    .module('ui')
    .run(runBlock);

  /** @ngInject */
  function runBlock($log, $timeout, Restangular, toastr, _) {
    
    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
    // if(response.status === 403) {
        // refreshAccesstoken().then(function() {
             // Repeat the request and then call the handlers the usual way.
      //       $http(response.config).then(responseHandler, deferred.reject);
    //         // Be aware that no request interceptors are called this way.
    //     });

   //      return false; // error handled
   //  }   
   
               function generalErrorHanding(response){
                       var errors = _.map(response.data, function(value, key){ return key + " " +value; });
                       var errorsum = errors.join("\n");
                       toastr.error('error', "application error: "+response.statusText, errorsum);
                       $log.error("custom error handler: "+response.status+" "+response.data);
               } 
   
              ///http://stackoverflow.com/a/34362876
              var generalHandlerTimer = $timeout(function(){
                 generalErrorHanding(response);
               },1);
               response.cancelGeneralHandler = function(){
                 $timeout.cancel(generalHandlerTimer);
               };
               return true; // error not handled
           }
        
     );
     $log.debug('runBlock end');
  
    
  }

})();
