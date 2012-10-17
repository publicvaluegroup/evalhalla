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

package object user {

class SecurityToken(h:HGHandle) extends HGPlainLink(h) with HGHandleHolder {
    @scala.reflect.BeanProperty
    var atomHandle:HGHandle = null  
}

@Path("/user")
@Produces(Array("application/json"))
class UserService {   
    @POST
    @Path("/login")    
    def authenticate(@FormParam("email") email:String, 
                     @FormParam("password") password:String):Json =
    {
        return evalhalla.transact(Unit => {
            var profile = db.retrieve(jobj("entity", "user", "email", email.toLowerCase()))
            if (profile == null || profile.is("disabled", true))
                return ko();
            else if (profile.has("validationCode"))
                return ko("validate");
            else if (profile.has("password") && !password.equals(profile.at("password").asString()))
                return ko();
            return ok.set("user", setSecurityToken(db.getHandle(profile), profile.dup()));    
        });
    }
    
    def setSecurityToken(userHandle:HGHandle, user:Json):Json = {
        var existingToken = hg.findOne(graph, 
                                   hg.and(hg.`type`(classOf[SecurityToken]), 
                                          hg.incident(userHandle)));
        if (existingToken != null)
            graph.remove(existingToken);
        var token = graph.getHandleFactory().makeHandle();
        graph.define(token, new SecurityToken(userHandle));
        return user.set("token", token.toString());        
    }    
}

}