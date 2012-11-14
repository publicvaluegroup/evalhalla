package evalhalla

import mjson.Json
import org.restlet.Request
import org.restlet.Response
import org.restlet.security.Authenticator
import org.restlet.Context
import org.restlet.data.Status
import org.restlet.util.Series
import org.hypergraphdb.HGQuery.hg
import org.hypergraphdb.HGHandle
import evalhalla.user.SecurityToken

/**
 * This is a filter applied on every request by Restlet. The authenticate method 
 * returns true/false whether the user is allowed or not. We return true almost all 
 * of the time, but as a side effect we set the user cookie to "anonymous" if 
 * there is something fishy. The only time we return false is if there's an attempt
 * to access the admin area without having admin permissions.
 */
class UserAuthenticator(context:Context) extends Authenticator(context) {

  protected override def authenticate(request: Request, response: Response): Boolean = { 
    var path = request.getResourceRef().getPath()
    var userid = request.getCookies().getFirstValue("user")
    var token = request.getCookies().getFirstValue("ehtk")
    var headers:Series[_] = request.getAttributes().get("org.restlet.http.headers").asInstanceOf[Series[_]]
        
    // Some browsers decide to send the header with the first letter capitalized, others
    // send it all lowercase event though on the client-side it's always set in lowercase.
    var token2 = headers.getFirstValue("ehtk")
    
    if (token2 == null)
        token2 = headers.getFirstValue("Ehtk")
        
        // Extra request header set only on AJAX call, otherwise cookie is enough. We could
        // transform the .ht requests into AJAX calls as well, but there's no need for it 
        // since their raw form doesn't contain any sensitive data (it should not!). 
    if (path.endsWith(".html") || 
        path.endsWith(".ht") || 
        path.endsWith(".js") ||
        path.endsWith(".ico") ||
        path.endsWith(".css") ||
        path.endsWith(".jpg") ||
        path.endsWith(".gif") ||
        path.endsWith(".png")) {
      
        token2 = token
    }
        
    var valid =  "anonymous".equals(userid) || 
        (userid != null && 
         userid.length() > 0 && 
         token != null && 
         token.length() == 36 &&
         token.equals(token2))
    if (!valid)
      request.getCookies().set("user", "anonymous")
    else if (!"anonymous".equals(userid)) {
      // Check that the token matches the one stored (we don't care about time stamps,
      // session expire on a browser close, so as long as the cookies are good, we're good.
      var userHandle = graph.getHandleFactory().makeHandle(userid)
      var existingToken:HGHandle = hg.findOne(graph, 
                                        hg.and(hg.`type`(classOf[SecurityToken]), 
                                               hg.incident(userHandle)))
      if (existingToken == null || !existingToken.getPersistent().toString().equals(token))
            request.getCookies().set("user", "anonymous")
    }
        
    if (path != null && (path.startsWith("/admin") || path.startsWith("/rest/admin"))) {
      val userid = request.getCookies().getFirstValue("user")
      if (isHandle(userid)) {
        val u:Json = handleToAtom(userid)
        return u != null && u.is("email", config.at(""))
      }
      else  
        return config.is("admin-email", userid)
    }
    else
        return true;
 
  }

  protected override def unauthenticated(request:Request, response:Response):Int =  {
    response.setStatus(Status.CLIENT_ERROR_FORBIDDEN)
    return org.restlet.routing.Filter.STOP
  } 
}