/*
Copyright (c) 2012, The Public Value Group, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met: 

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer. 
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those
of the authors and should not be interpreted as representing official policies, 
either expressed or implied, of the FreeBSD Project. 
*/

/**
 * User login/registration module.
 */
angular.module('ahgUser',[]).controller('ahgUserController',
function ($scope, $http, $rootScope) {
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
  
  /**
   * Perform login by setting cookies, and initializing globally scoped
   * variables relevant for user management.
   */
  $scope.setUser = function(profile) {
    $rootScope.user = profile;    
    var cook = {path:"/"};
    if ($scope.rememberUser)
        cook.expire = "100*365";
    $.cookie("user", $rootScope.user.hghandle, cook);
    $.cookie("ehtk", $rootScope.user.token, cook);
    $http.defaults.headers.common['ehtk'] = $rootScope.user.token;
    if ($scope.rememberUser)
        $.cookie("rememberUser", true);
    else
        $.cookie("rememberUser", false);
    $('#loginErrorDiv').html('');
    $scope.showLogin = false;            
  };
  
  /**
   * Handler of the Login button. 
   */
  $scope.doLogin = function() {
    $http.post('/rest/user/login', 
        {email:$scope.user.email, 
         password:$scope.user.password}).success(
    function(x) {
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
  
  /**
   * Handler of the Logout button. Clear cookies and global user variable.
   */
  $scope.doLogout = function() {
    $.cookie("user", 'anonymous', { path: '/' });
    $.cookie("ehtk", null, { path: '/' });
    $rootScope.user = {email:''};
  };
  
  /**
   * Utility to show/hide UI things.
   */
  $scope.isLoggedIn = function() {
    return $rootScope.user && 
           $rootScope.user.email!=''              
  };
  
  /**
   * Handler of the Register button. Submits the registration form, and then
   * lets the user copy&paste to emailed validation token. 
   */
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
  
  /**
   * Handler of the Validate button of user registration - submit validation
   * back to the sever to complete validation. If successful, login the user
   * immediately.
   */
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
  
  /**
   * Ask the server for a re-send of the validation token. 
   */
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
});