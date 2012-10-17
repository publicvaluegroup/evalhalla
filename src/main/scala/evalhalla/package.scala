package object evalhalla {
  import mjson.Json

  import org.hypergraphdb.HyperGraph
  import org.hypergraphdb.HGConfiguration
  import mjson.hgdb.HyperNodeJson
  import mjson.hgdb.JsonTypeSchema
  import org.hypergraphdb.HGEnvironment
  
  val True = java.lang.Boolean.TRUE;
  val False = java.lang.Boolean.FALSE;
  var graph:HyperGraph = null
  var db:HyperNodeJson = null
  var config:Json = Json.`object`("port", 8182:java.lang.Integer)
  
  def init(cfg:Json = null) = {
    if (cfg != null)
        evalhalla.config = cfg;
    graph = HGEnvironment.get(config.at("dbLocation").asString())
    var hgconfig = new HGConfiguration()
    hgconfig.getTypeConfiguration().addSchema(new JsonTypeSchema())
    registerIndexers
    db = new HyperNodeJson(graph);
  }
  
  def ok:Json = Json.`object`("ok", True);
  def ko(error:String = "Error occured") = Json.`object`("ok", False, "error", error);
  
  def ensureTx[T](code:Unit => T) = {
    graph.getTransactionManager().ensureTransaction(new java.util.concurrent.Callable[T]() {
      def call:T = { 
        return code()
      }
    });
  }

  def transact[T](code:Unit => T) = {
    graph.getTransactionManager().transact(new java.util.concurrent.Callable[T]() {
      def call:T = { 
        return code()
      }
    });
  }
  
  private[evalhalla] def registerIndexers:Unit = {
//        ByJsonPropertyIndexer idx = 
//            new ByJsonPropertyIndexer("time", 
//                                      graph.getTypeSystem().getTypeHandle(
//                                          JavaTypeSchema.classToURI(Double.class)));
//        idx.setType(JsonTypeSchema.objectTypeHandle);
//        graph.getIndexManager().register(idx);
  }
}