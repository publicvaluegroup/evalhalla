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
package evalhalla

import javax.ws.rs._
import javax.ws.rs.core._
import mjson.Json
import mjson.Json._
import mjson.Json.{`object`=>jobj};

/**
 * Base class for JSR 311 REST services. The purpose is to contain some
 * contextual information for the request and offer some utility
 * methods to access it. An example of such utility method is getting
 * the current user which is sent with every request as a header cookie. 
 */
class RestService {
    @Context 
    var httpHeaders:HttpHeaders = null    
    @Context 
    var uriInfo:UriInfo = null
    @Context 
    var request:Request = null
    
    def getUserId():String = {
      var cookie:Cookie = httpHeaders.getCookies().get("user")
      if (cookie != null && cookie.getValue() != null && cookie.getValue().length() > 0)
          cookie.getValue()
      else
          "anonymous"
    }
    
    def getUser():Json =  {
      var id = getUserId()
      if ("anonymous".equals(id))
        jobj("entity", "user", "email", "anonymous");
      else
        evalhalla.handleToAtom(id)            
    }
    
    def isAdmin():Boolean = {
        var user = getUser()        
        user.is("email", evalhalla.config.at("admin-email"))
    }   
}