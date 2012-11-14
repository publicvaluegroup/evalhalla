/**
 * eValhalla Application AngularJS main module.
 *
 */

var evalhalla = {};
var app = angular.module('evalhalla', ['ui','ngView']);

app.directive('formatHelp', function() {
    return {
        link : function(scope, elm, attrs, ctrl) {
            elm.wrap("<div class='controls'></div>");
            $('<div class="input-help"><h4>' + attrs.formatHelp +
                '</h4></div>').insertAfter(elm);
        }
    }
});

angular.module('ngView', [], function($routeProvider, $locationProvider) {
});

function ahgUserController($scope, $http, $rootScope) {
  httpclient=$http;
  $rootScope.user = {email:''};
  var username = $.cookie("user");
  if (username) {
    $http.post('/rest/user/autologin',
        {user:username, token:$.cookie("ehtk")}).success(function(x) {
          if (x.ok) $rootScope.user = x.profile;
          else $scope.doLogout();
        });
  }
  $scope.user = {firstName:'', lastName:'', email:'', password:''};    
  $scope.samePwd = function(value) { return value==$('#newpwd').val(); }
  $scope.showRegister = false;
  $scope.showLogin = false;
  $scope.showValidateToken = false;
  $scope.validationToken = '';
  $scope.rememberUser = $.cookie("rememberUser");
  $scope.setUser = function(profile) {
    $rootScope.user = profile;    
    var cook = {path:"/"};
    if ($scope.rememberMe)
        cook.expire = "100*365";
    $.cookie("user", $rootScope.user.hghandle, cook);
    $.cookie("ehtk", $rootScope.user.token, cook);
    $http.defaults.headers.common['ehtk'] = $rootScope.user.token;
    $('#loginErrorDiv').html('');
    $scope.showLogin = false;            
  };
  $scope.doLogin = function() {
    $http.post('/rest/user/login', {email:$scope.user.email, password:$scope.user.password}).success(function(x) {
      if (x.ok) {
        $scope.setUser(x.profile);
      }
      else if (x.error == 'validate') {
        $scope.showLogin = ! ($scope.showValidateToken = true); 
      }
      else {
        $('#loginErrorDiv').html(x.error);
      }
    });
  };
  $scope.doLogout = function() {
    $.cookie("user", 'anonymous', { path: '/' });
    $.cookie("ehtk", null, { path: '/' });
    $rootScope.user = {email:''};
  };
  $scope.isLoggedIn = function() {
    return $rootScope.user && 
           $rootScope.user.email!=''              
  };
  $scope.startRegister = function() {
    if ($scope.registerForm.$invalid) {
      $('.ng-invalid + .input-help').show();
    }
    $http.post('/rest/user/register', $scope.user).success(function (x) {
      $scope.showRegister = false;
      if (x.ok) {
        $("<p>Please check your email and click on the validation " +
          "link to complete your registration.</p>").bootstrap_dialog({
          title: "Please Complete Registration", 
          ok: function(){ $(this).bootstrap_dialog("close"); }
          });                    
      }
      else {
        $("<p>Registration failed with the following error: " + 
              x.error + ".</p>").bootstrap_dialog({
          title: "Error", 
          ok: function(){ $(this).bootstrap_dialog("close"); }
          });          
      }
    });
  };
  $scope.validateRegistration = function() {
    $http.post('/rest/user/validatetoken', 
        {token:$scope.validationToken, email:$scope.user.email}).success(function(x) {
      if (x.ok) {
        $scope.showValidateToken=false;
        $scope.setUser(x.profile);
      }      
      else 
        $('#validationErrorDiv').html(x.error)
    });
  };
  $scope.resendRegistrationConfirmation = function() {
    $http.post('/rest/user/resendtoken', {email:$scope.user.email}).success(function(x) {
      if (x.ok) {
        $("<p>An email with your validation code has been sent to " + 
              $scope.user.email + ".</p>").bootstrap_dialog({
          title: "Information", 
          ok: function(){ $(this).bootstrap_dialog("close"); }
          });          
      }
      else 
        $('#validationErrorDiv').html(x.error)
    });
  };
}

function MainController($scope, $http) {
}
