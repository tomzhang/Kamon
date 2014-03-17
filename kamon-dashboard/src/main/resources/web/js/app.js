'use strict';

var kamonDasboard = angular.module('dashboard', ['dashboard.services', 'ui.bootstrap', 'nvd3ChartDirectives', 'ui.sortable', 'ngRoute']);
//
kamonDasboard.config(function ($routeProvider) {
    $routeProvider.
        when('/jvm', {
            templateUrl: 'templates/jvm.html',
            controller: JvmController
        }).
        when('/play', {
            templateUrl: 'templates/play.html',
            controller: PlayController

        }).
        when('/spray', {
            templateUrl: 'templates/spray.html',
            controller: SprayController
        }).
        otherwise({
            redirectTo: '/jvm'
        });
});


//funtion to fade left panel
$(document).ready(function () {
    $(".alert").addClass("in").fadeOut(4500);
});

