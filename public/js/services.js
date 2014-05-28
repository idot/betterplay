/*global define */

'use strict';

/** global constants **/
var DATEFILTER = 'dd/MM HH:mm';

//TODO: check if closing time should not come from the server somehow to prevent submission rejected errors
//I now added one additional minute
//m s ms
var MSTOCLOSING = 61 * 60 * 1000;


/** global constants **/

define(['angular'], function(angular) {

/* Services */

// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', [])
  .value('version', '0.1');
});