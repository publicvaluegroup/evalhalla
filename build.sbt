name := "evalhalla"

version := "1.0"

resolvers += "HyperGraphDB" at "http://www.hypergraphdb.org/maven"

resolvers += "Restlets" at "http://maven.restlet.org"

libraryDependencies += "org.hypergraphdb" % "hgdbmjson" % "1.2"

libraryDependencies += "org.hypergraphdb" % "hgbdbje" % "1.2"

libraryDependencies += "org.restlet.jse" % "org.restlet.ext.jaxrs" % "2.1.0"

// libraryDependencies += "org.openid4java" % "openid4java" % "0.9.6"
