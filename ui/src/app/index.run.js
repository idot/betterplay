(function() {
  'use strict';

  angular
    .module('ui')
    .run(runBlock);

  /** @ngInject */
  function runBlock($log, $timeout, Restangular, toastr, _, virtualContent, $rootScope) {
    
    Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
    // if(response.status === 403) {
        // refreshAccesstoken().then(function() {
             // Repeat the request and then call the handlers the usual way.
      //       $http(response.config).then(responseHandler, deferred.reject);
    //         // Be aware that no request interceptors are called this way.
    //     });

   //      return false; // error handled
   //  }   
              var mapErrors = function(errors){
                  if(errors.error){
                      if(typeof errors.error == "string"){
                          return "error: "+errors.error;
                      }
                      return _.map(errors.error, function(value, key){ return key + ": " +value; }).join("\n");
                  }else{                    
                    var errorsp = _.map(errors, function(value, key){ return key + ": " +value; });
                    var errorstring = errorsp.join("\n");  
                    return errorstring;
                 }
               };
   
              var tryParse = function(data){ 
                  try {
                          var errors=JSON.parse(response.data);
                          return mapErrors(errors);
                  } catch(e) {
                          if(response.data && response.data.indexOf("html") < 0){
                               if(response.data.length > 70){
                                   return response.data.substring(0, 70);
                               }else{
                                   return response.data;
                               }
                          }
                          return "";
                  }               
               };
   
               function generalErrorHanding(response){
                   var errorstring = "";
                   var datType = typeof(response.data);
                   switch( datType ){
                       case "string": 
                           errorstring = tryParse(response.data); break;
                       case "object":
                           errorstring = mapErrors(response.data); break;
                       default:
                           $log.error("unforseen obj type: "+ datType);   
                   }
                   toastr.error(errorstring, "error: "+response.statusText);
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
     
    
     $rootScope.virtCont = virtualContent;

     
     $log.debug('runBlock end');
  
    
  }

})();
