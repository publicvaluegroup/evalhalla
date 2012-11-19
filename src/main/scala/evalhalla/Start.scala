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
import java.io.File

/**
 * Contains the main application program. Reads configuration and sets up
 * the HTTP rest server using the Restlet framework. 
 */
object Start {

  import org.restlet._;

  import org.restlet.data.Protocol;
  import org.restlet.ext.jaxrs.JaxRsApplication;
  import org.restlet.resource.Directory;
  import org.restlet.routing.Redirector;
  import org.restlet.routing.Router;
  import org.restlet.routing.Template;
 
  def main(args: Array[String]): Unit = {
    if (args.length == 1) {
        var source:scala.io.Source = null;
        try {
            source = scala.io.Source.fromFile(args(0))
            val lines = source.mkString
            println(lines);
            evalhalla.init(mjson.Json.read(lines))
        }
        finally {
          if (source != null)
            source.close()
        }
    }
    else if (args.length == 0) {
      // default configuration, use current directory as a base
      var rootDir:File = new java.io.File(".")
      println("No config file provided, using defaults at root " + rootDir.getCanonicalPath())
      config.set("dbLocation", new File(rootDir, "db").getCanonicalPath())
      config.set("siteLocation", new File(new File(rootDir, "src"), "main").getCanonicalPath())
      evalhalla.init()
    }
    else {
      println("Usage Start json_config_file");
      System.exit(-1);      
    }        
    var server = new Component();
    server.getClients().add(Protocol.HTTP);
    server.getClients().add(Protocol.FILE);
    var servicesApplication = 
        new JaxRsApplication(server.getContext().createChildContext());
    servicesApplication.add(new EValhallaApplication());

    var router = new org.restlet.routing.Router(server.getContext().createChildContext());
    router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
    
    var html = new org.restlet.Application(server.getContext().createChildContext()) {  
        override def createInboundRoot:Restlet = {  
            var dir = new Directory(getContext().createChildContext(), "file:" + config.at("siteLocation").asString() + "/html/");
            dir.setIndexName("index.html");
            return dir;
        }
    };
    router.attach("/", html).setMatchingMode(Template.MODE_STARTS_WITH);
    
    var jsapp = new org.restlet.Application(server.getContext().createChildContext()) {  
        override def createInboundRoot:Restlet = {  
            return new Directory(getContext().createChildContext(), "file:" + config.at("siteLocation").asString() + "/javascript/");
        }
    };               
    router.attach("/javascript", jsapp).setMatchingMode(Template.MODE_STARTS_WITH);
    
    router.attach("/rest", servicesApplication).setMatchingMode(Template.MODE_STARTS_WITH);
    router.setRoutingMode(Router.MODE_BEST_MATCH);
    val auth:UserAuthenticator = new UserAuthenticator(router.getContext());
    auth.setNext(router);
    
    server.getDefaultHost().attach(auth);
    server.getServers().add(Protocol.HTTP, 8182);
    server.start();
    
    println("Started with DB " + evalhalla.graph.getLocation);
  }
}

/**
 * The JSR 311 Application implementation that provides a list of
 * all the REST services available. 
 */
class EValhallaApplication extends javax.ws.rs.core.Application {
  def getClasses() : java.util.Set[Class[_]] = {       
    var S = new java.util.HashSet[Class[_]]
    S.add(classOf[evalhalla.DataService]);    
    S.add(classOf[evalhalla.user.UserService]);
    S.add(classOf[evalhalla.JsonEntityProvider]);
    return S;
  }
}