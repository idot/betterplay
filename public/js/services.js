/*global define */

'use strict';

/** global constants **/
var DATEFILTER = 'dd/MM HH:mm';

/**var MINUTESTOCLOSING = 60 * 1000;**/


/** global constants **/

define(['angular'], function(angular) {

/* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', [])
  .value('version', '0.1');
});