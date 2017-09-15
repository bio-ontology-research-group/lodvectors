@Grab(group='com.github.albaker', module='GroovySparql', version='0.9.0')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')

import groovyx.net.http.HTTPBuilder
import groovy.json.*
import java.net.*
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.*


		   //http://jagannath.pdn.cam.ac.uk:9200/vector/_search?q=http://purl.uniprot.org/uniprot/Q9TNU1&size=1
def search(def id) {
  def url = 'http://127.0.0.1:9200/'
  def http = new HTTPBuilder(url)
  def fres = ""
  def fres2 = null
  http.request(GET, JSON) { req ->
    uri.path = '/vector/_search' // overrides any path in the default URL
    uri.query = [ size:'1', q: "name:\""+id+"\"" ]
    response.success = { resp, json ->
      //      fres = json.hits.hits[0]._source."@model_factor".split(" ").collect { new Double(it.split("\\|")[1]) }
      if (json && json.hits && json.hits.hits && json.hits.hits[0]) {
	fres2 = json.hits.hits[0]._source."@model_factor"
      }
    }
  }
  http.shutdown()
  return fres2
}

def similar(def id) {
  def vector = search(id)
  if (vector) {
    vector = vector.split(" ").collect { it.split("\\|")[1]}
    def url = 'http://127.0.0.1:9200/'
    def http = new HTTPBuilder(url)
    def query = """
{
    "query": {
        "function_score": {
            "query" : {
                "query_string": {
                    "query": "*"
                }
            },
            "script_score": {
                "script": {
                    "inline": "payload_vector_score",
                "lang": "native",
                "params": {
                    "field": "@model_factor",
                    "vector": """+vector+""",
                    "cosine" : true
                    }
}
            },
            "boost_mode": "replace"
        }
    }
}
"""
    def jsonSlurper = new JsonSlurper()
    def js = new JsonBuilder(jsonSlurper.parseText(query))
    def t
    http.post( path: '/vector/_search', requestContentType : JSON, body: js.toPrettyString() ) { resp, reader -> t = reader }
    http.shutdown()
    def rmap = [:]
    t.hits.hits.each { hit ->
      def rid = hit._source.name
      rmap[rid] = hit._score
    }
    rmap
  } else {
    null
  }
}

if(!application) {
  application = request.getApplication(true)
}

response.contentType = 'application/json'
def query = params.query
def qtype = params.type
def smart = params.smart
def dumb = ((smart == null) || (smart?.toLowerCase() != "true"))

if (dumb) {
  if (qtype && qtype == "getvec") {
    def result = search(query)
    if (result) {    
      println result.split(" ").collect { it.split("\\|")[1] }
    } else {
      println ""
    }
  } else {
    def result = similar(query)
    if (result) {
      def builder = new JsonBuilder(result)
      println builder.toPrettyString()
    } else {
      println "{}"
    }
  }
} else { // smart
  if (qtype && qtype == "getvec") {
    def result = search(query)
    
    def jMap = [:]
    if (result) {
      jMap.results = result.split(" ").collect { it.split("\\|")[1] }
    } else {
      jMap.results = [:]
    }
    jMap."@context" = "https://raw.githubusercontent.com/bio-ontology-research-group/lodvectors/master/lodvector.jsonld"
    jMap."meta" = [
      "prov:wasGeneratedBy": "https://github.com/bio-ontology-research-group/lodvectors/blob/master/sim.groovy",
      "prov:generatedAt": new Date(),
      "errors": [],
      "warnings": [ "North Korean missile incoming." ],
      "resultCount": result.size(),
      "URLcalled": request.getRequestURL()+"?"+request.getQueryString()
    ]
    def builder = new JsonBuilder(jMap)
    response.contentType = 'application/json'
    println builder.toPrettyString()
  } else {
    def result = similar(query)
    def jMap = [:]
    if (result) {
      jMap.results = result
    } else {
      jMap.results = [:]
    }
    jMap."@context" = "https://raw.githubusercontent.com/bio-ontology-research-group/lodvectors/master/lodvector.jsonld"
    jMap."meta" = [
      "prov:wasGeneratedBy": "https://github.com/bio-ontology-research-group/lodvectors/blob/master/sim.groovy",
      "prov:generatedAt": new Date(),
      "errors": [],
      "warnings": [ "North Korean missile incoming." ],
      "resultCount": result.size(),
      "URLcalled": request.getRequestURL()+"?"+request.getQueryString()
    ]
    def builder = new JsonBuilder(jMap)
    println builder.toPrettyString()
  }
}
