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
 * Provides direct access to the database. 
 */
@Path("/data")
@Produces(Array("application/json"))
class DataService {

  @POST
  @Path("/entity")
  @Consumes(Array("application/json"))
  def createEntity(data:Json):Json = {
    db.addTopLevel(data)
    return ok().set("entity", data)
  }
  
  @DELETE
  @Path("/entity/{handle}")
  def removeEntity(@PathParam("handle") handleAsString:String):Json = {
    var handle = graph.getHandleFactory().makeHandle(handleAsString)
    db.remove(handle)
    return ok()
  }
  
  @PUT
  @Path("/entity/{hghandle}")
  def setEntity(@PathParam("hghandle") handleAsString:String, data:Json):Json = {
    if (!evalhalla.isHandle(handleAsString))
      return ko("HyperGraphDB handle expected as last path component.") 
    var handle = evalhalla.asHandle(handleAsString)
    db.replace(handle, data, db.getType(handle))
    return ok()
  }

  @GET
  @Path("/entity/{hghandle}")
  def getEntity(@PathParam("hghandle") handleAsString:String):Json = {
    if (!evalhalla.isHandle(handleAsString))
      return ko("HyperGraphDB handle expected as last path component.") 
    var entity = db.get(evalhalla.asHandle(handleAsString))
    if (entity == null)
      return ko("Not found.")
    else
      ok().set("entity", entity)    
  }

  @GET
  @Path("/list")
  def listEntities(@QueryParam("pattern") patternParam:String):Json = {
    var pattern:Json = Json.read(patternParam);        
    return db.getAll(pattern);    
  }  
}