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
package object evalhalla {

  import mjson.Json
  import mjson.Json.{`object`=>jobj};

  import org.hypergraphdb.HyperGraph
  import org.hypergraphdb.HGHandle
  import org.hypergraphdb.transaction.HGUserAbortException
  import org.hypergraphdb.HGConfiguration
  import mjson.hgdb.HyperNodeJson
  import mjson.hgdb.JsonTypeSchema
  import org.hypergraphdb.HGEnvironment
  import java.util.regex.Pattern
  
  val HANDLE_REGEX:Pattern  = Pattern.compile("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}", Pattern.CASE_INSENSITIVE)  
  val True = java.lang.Boolean.TRUE;
  val False = java.lang.Boolean.FALSE;
  var graph:HyperGraph = null
  var db:HyperNodeJson = null
  var config:Json = Json.`object`("port", 8182:java.lang.Integer)
  
  def init(cfg:Json = null) = {
    if (cfg != null)
        evalhalla.config = cfg
    val hgconfig = new HGConfiguration()
    hgconfig.getTypeConfiguration().addSchema(new JsonTypeSchema())
    graph = HGEnvironment.get(config.at("dbLocation").asString(), hgconfig)
    registerIndexers
    db = new HyperNodeJson(graph)
  }
  
  def ok():Json = Json.`object`("ok", True);
  def ko(error:String = "Error occured") = Json.`object`("ok", False, "error", error);
  
  def ensureTx[T](code:Unit => T) = {
    graph.getTransactionManager().ensureTransaction(new java.util.concurrent.Callable[T]() {
      def call:T = { 
        return code()
      }
    });
  }

  def transact[T](code:Unit => T):T = {
    var result:T = null.asInstanceOf[T]
    var done = false
    var commit = true
    while (!done) {
      graph.getTransactionManager().beginTransaction()
      try {
        result = code()
        commit = true;
      } 
      catch {
        case e:HGUserAbortException => {
          commit = false
          done = true
        }
        case e:scala.runtime.NonLocalReturnControl[T] => {            
            result = e.value
            commit = true;
        }
        case e => {
          commit = false          
          if (!graph.getStore().getTransactionFactory().canRetryAfter(e))
            throw e
        }
      }     
      finally try {
        graph.getTransactionManager().endTransaction(commit)
        done = done || commit
      } 
      catch {
        case e =>  {
          if (!graph.getStore().getTransactionFactory().canRetryAfter(e))
            throw e
        }        
      }
    }
    return result
  }
  
  def isHandle(s:String):Boolean = { HANDLE_REGEX.matcher(s).matches() }  
  def handleToAtom[T](h:String):T = { db.get(graph.getHandleFactory().makeHandle(h)) }
  def asHandle(h:String):HGHandle = { graph.getHandleFactory().makeHandle(h) }
  
  def email(to:String, subject:String, body:String):Unit = {
    email(config.at("from-email").asString(), to, subject, body);
  }
  
  def email(from:String, to:String, subject:String, body:String):Unit = {
      MailClient.getInstance().sendEmail(from, to, subject, body);
  }
  
  private[evalhalla] def registerIndexers:Unit = {    
  }
}