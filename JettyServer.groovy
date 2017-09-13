@Grapes([
          @Grab(group='javax.servlet', module='javax.servlet-api', version='3.1.0'),
          @Grab(group='org.eclipse.jetty', module='jetty-server', version='9.3.13.M0'),
          @Grab(group='org.eclipse.jetty', module='jetty-servlet', version='9.3.13.M0'),
          @Grab(group='org.eclipse.jetty', module='jetty-servlets', version='9.3.13.M0'),
	  @GrabConfig(systemClassLoader=true)
        ])

@Grab(group='javax.el', module='javax.el-api', version='3.0.0')

import javax.servlet.*
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.*
import groovy.servlet.*
import org.eclipse.jetty.servlets.*
				 // Add the filter, and then use the provided FilterHolder to
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
  context.addServlet(GroovyServlet, '/sim.groovy')
  context.setAttribute('version', '0.1')
  context.setAttribute('vmap', vmap)
  FilterHolder cors = context.addFilter(CrossOriginFilter.class,"/*",EnumSet.of(DispatcherType.REQUEST))
  cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
  cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
  cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD");
  cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin");
  
  server.start()
}

startServer()
