package evalhalla

import javax.ws.rs._
import java.io.File

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
            source = scala.io.Source.fromFile("file.txt")
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
    
    server.getDefaultHost().attach(router);
    server.getServers().add(Protocol.HTTP, 8182);
    server.start();
    
    println("Started with DB " + evalhalla.graph.getLocation);
  }
}

class EValhallaApplication extends javax.ws.rs.core.Application {
  def getClasses() : java.util.Set[Class[_]] = {       
    var S = new java.util.HashSet[Class[_]]
    S.add(classOf[evalhalla.user.UserService]);
    return S;
  }
}