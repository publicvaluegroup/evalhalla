package evalhalla

import javax.ws.rs._
import javax.ws.rs.core._
import mjson.Json
import mjson.Json._
import mjson.Json.{`object`=>jobj};

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