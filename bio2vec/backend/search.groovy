@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' )

import groovyx.net.http.HTTPBuilder
import groovy.json.*
import java.net.*
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.*

if(!application) {
  application = request.getApplication(true)
}

response.contentType = 'application/json'
def q = params.term

//println "hello"
url = 'http://10.254.145.46:9200/'
http = new HTTPBuilder(url)

println new JsonBuilder(search(q))

def search(def id) {
  def fres = ""
  def fres2 = null
  def query = """
{ 
  "query": {
    "prefix" : { "names" : "$id" }
  },
  "size" : 25
}
"""
  def jsonSlurper = new JsonSlurper()
  def js = new JsonBuilder(jsonSlurper.parseText(query))
  def t
  http.post( path: '/bio2vec/_search', requestContentType : JSON, body: js.toPrettyString() ) { resp, reader -> t = reader }
  return t.hits.hits
}
