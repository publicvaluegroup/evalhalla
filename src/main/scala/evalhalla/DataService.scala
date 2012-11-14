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

@Path("/data")
@Produces(Array("application/json"))
class DataService {

  @DELETE
  @Path("/{handle}")
  def removeEntity(@PathParam("handle") handleAsString:String):Json = {
    var handle = graph.getHandleFactory().makeHandle(handleAsString)
    db.remove(handle)
    return ok()
  }
  
  @GET
  @Path("/list")
  def listEntities(@QueryParam("pattern") patternParam:String):Json = {
    var pattern:Json = Json.read(patternParam);        
    return db.getAll(pattern);    
  }  
}