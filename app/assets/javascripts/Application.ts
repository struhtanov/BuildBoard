/// <reference path='_all.ts' />


module buildBoard {
    'use strict';

    angular.module('buildBoard', ['ngRoute','ui.bootstrap'])
        .service(BackendService.NAME, BackendService)
        .service(BranchesService.NAME, BranchesService)
        .controller('branchesController', BranchesController)
        .controller(BranchLineController.NAME, BranchLineController)
        .filter('status', status)
        .filter('activeFilter', activeFilter)
        .filter('encode', encode)
        .filter('pullRequestStatus', pullRequestStatus)
        .directive('entityTitle', ()=>new EntityTitleDirective())
        .directive(EntityStateDirective.NAME, ()=>new EntityStateDirective())
        .directive(PanelDirective.NAME, ()=>new PanelDirective())
        .directive(PanelHeadingDirective.NAME, ()=>new PanelHeadingDirective())
        .directive(PanelBodyDirective.NAME, ()=>new PanelBodyDirective())
        .config(['$routeProvider',
            ($routeProvider:ng.route.IRouteProvider)=>
                $routeProvider
                    .when('/branchList/:filter', {
                        templateUrl: '/assets/partials/main.html',
                        controller: BranchesController
                    })
                    .when('/branches/:branchId', {
                        templateUrl: '/assets/partials/branch.html',
                        controller: BranchController
                    })
                    .when('/branches/:branchType/:branchId', {
                        templateUrl: '/assets/partials/branch.html',
                        controller: BranchController
                    })
                    .otherwise({
                        redirectTo: '/branchList/all'
                    })
        ])


}