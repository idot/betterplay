'use strict';

var path = require('path');
var gulp = require('gulp');
var conf = require('./conf');

gulp.task('build:copy-flags', function(){
  return gulp.src(conf.wiredep.directory+'/flag-icon-css/flags/*')
    .pipe(gulp.dest(path.join(conf.paths.dist, '/flags/')));
});