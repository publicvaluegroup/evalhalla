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


import evalhalla._
import mjson.Json
import mjson.Json._
import mjson.Json.{`object`=>jobj};
import org.hypergraphdb.HGPlainLink
import org.hypergraphdb.HGHandleHolder
import org.hypergraphdb.HGHandle
import org.hypergraphdb.HGQuery.hg

/**
 * The user package implements the main service handling user management, including
 * login, registration and profile management.
 * 
 * Emails are used as usernames and to ensure case-insensitivity, they are all
 * converted to lower case at the beginning of each method.
 */
package object user {


class SecurityToken(h:HGHandle) extends HGPlainLink(h) with HGHandleHolder {
    @scala.reflect.BeanProperty
    var atomHandle:HGHandle=null
}

@Path("/user")
@Produces(Array("application/json"))
@Consumes(Array("application/json"))
class UserService extends RestService {
  
    def normalize(data:Json):Json = {
      if (data.has("email"))
        data.set("email", data.at("email").asString().toLowerCase())
      return data
    }
    
    def setSecurityToken(userHandle:HGHandle, user:Json):Json = {
        var existingToken:HGHandle = hg.findOne(graph, hg.and(hg.`type`(classOf[SecurityToken]), 
                                                 hg.incident(userHandle)));
        if (existingToken != null)
            graph.remove(existingToken)
        var token = graph.getHandleFactory().makeHandle()
        graph.define(token, new SecurityToken(userHandle))
        return user.set("token", token.toString())        
    }
  
    @POST
    @Path("/login")
    def authenticate(data:Json):Json = {
      normalize(data);
      var profile = db.retrieve(jobj("entity", "user", 
                                "email", data.at("email")))
      if (profile == null || !profile.is("password", data.at("password")))
        return ko("Invalid user or password mismatch.")
      else if (profile.has("validationToken"))
        return ko("validate")
      return ok().set("profile",  setSecurityToken(db.getHandle(profile), profile.dup()))
    }
    
    @POST
    @Path("/autologin")
    def autologin(data:Json):Json = {
      if (!data.has("token") || data.at("token").isNull())
        return ko()
      var token = asHandle(data.at("token").asString())
      var userHandle = asHandle(data.at("user").asString())
      var existingToken:HGHandle = hg.findOne(graph, 
                                     hg.and(hg.`type`(classOf[SecurityToken]), 
                                            hg.incident(userHandle)))
      if (existingToken == null || !existingToken.equals(token)) {      
        return ko();
      }
      else {
        var profile:Json = db.get(userHandle)
        return ok().set("profile", profile)
      }
    }
    
    @POST
    @Path("/register")
    def register(data:Json):Json = {
      return transact(Unit => {
        normalize(data);
        // Check if we already have that user registered
        var profile = db.retrieve(jobj("entity", "user", 
                                       "email", data.at("email")))
        if (profile != null)
          return ko().set("error", "duplicate")
        // Email validation token
        var token = graph.getHandleFactory().makeHandle().toString()
        db.addTopLevel(data.set("entity", "user")
                           .set("validationToken", token)
                           .delAt("passwordrepeat"))
        evalhalla.email(data.at("email").asString(), 
                      "Welcome to eValhalla - Registration Confirmation",
                      "Please validate registration with " + token)
        return ok
      })
    }
    
    @POST
    @Path("/validatetoken")
    def validateToken(data:Json):Json = {
      normalize(data);
      if (!data.has("email") || !data.has("token"))
        ko()
      else {
        var profile = db.retrieve(jobj("entity", "user", 
                                       "email", data.at("email")))
        if (profile == null)
          ko()
        else return evalhalla.transact(Unit => {
          if (profile.is("validationToken", data.at("token"))) { 
            profile.delAt("validationToken")
            db.unique(profile)
            ok().set("profile", profile)
          }
          else
            ko("Validation token doesn't match.")
        });
      }
    }
    
    @POST
    @Path("/resendtoken")
    def resendToken(data:Json):Json = {
      normalize(data)
      if (!data.has("email"))
        ko("No email specified.")
      else {
        var profile = db.retrieve(jobj("entity", "user", 
                                       "email", data.at("email")))
        if (profile == null)
          ko("Unknown email.")
        else {
          email(
                config.at("from-email").asString(),
                profile.at("email").asString(), 
                "Your eValhalla Registration Validation Token",
                "Please copy and paste the following:" + 
                   profile.at("validationToken").asString());
          ok()
        }
      }
    }
}
}