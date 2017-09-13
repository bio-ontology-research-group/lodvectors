@Grapes([
          @Grab(group='javax.servlet', module='javax.servlet-api', version='3.1.0'),
          @Grab(group='org.eclipse.jetty', module='jetty-server', version='9.3.0.M2'),
          @Grab(group='org.eclipse.jetty', module='jetty-servlet', version='9.3.0.M2'),
        ])

//@Grab(group='javax.el', module='javax.el-api', version='3.0.0')
 
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*

def startServer() {
  def map = [:]
  def rmap = [:]
  new File("uniprot.map").splitEachLine("\t") { line ->
    def iri = line[0]?.replaceAll("<","")?.replaceAll(">","")
    def id = line[1]
    map[iri] = id
    rmap[id] = iri
  }
  
  def vmap = [:]
  new File("uniprot.vec").splitEachLine(" ") { line ->
    def id = line[0]
    def iri = rmap[id]
    def vec = line[1..-1].collect { new Double(it) }
    vmap[iri] = vec
  }
  

  def server = new Server(31337)
  def context = new ServletContextHandler(server, '/', ServletContextHandler.SESSIONS)
  context.resourceBase = '.'
  context.addServlet(GroovyServlet, '/tsne.groovy')
  context.setAttribute('version', '0.1')
  context.setAttribute('vmap', vmap)
  server.start()
}

startServer()
