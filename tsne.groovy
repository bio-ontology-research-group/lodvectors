@Grab(group='com.github.albaker', module='GroovySparql', version='0.9.0')
@Grab(group='com.jujutsu.tsne', module='tsne-demos', version='2.4.0')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')


import groovy.json.*
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.*

import groovy.sparql.*
import com.jujutsu.tsne.barneshut.*
import com.jujutsu.tsne.*
import com.jujutsu.utils.*
import java.util.stream.*
import org.math.plot.*
import org.math.plot.plots.*

def sparql = new Sparql(endpoint:"http://sparql.uniprot.org/")

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


def cosine = { x, y ->
  double dotProduct = 0.0
  double normA = 0.0
  double normB = 0.0
  for (int i = 0; i < x.size(); i++) {
    dotProduct += x[i] * y[i]
    normA += Math.pow(x[i], 2)
    normB += Math.pow(y[i], 2)
  }   
  dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
}




if(!application) {
  application = request.getApplication(true)
}

//def vmap = context.getAttribute('vmap')

try {
  def sparqlQuery = request.getParameter('query')
  if (sparqlQuery && sparqlQuery.size()>0) {
    def x = []
    def dim = 0
    def labels = []
    def jMap = [:]
    jMap.head = [:]
    jMap.head.vars = []
    jMap.results = [:]
    jMap.results.bindings = []
    def first = true
    sparql.eachRow sparqlQuery, { row ->
      def cm = [:]
      row.each {k, v ->
	if (first) { // add headers
	  jMap.head.vars << k
	}
	// add content
	cm."$k" = [:]
	cm."$k"."value" = v
	cm."$k"."type" = (v instanceof String && v.indexOf(" ")==-1)?"uri":"string"
	def vec = search(v)
	if (vec) {
	  def vecvec = vec.split(" ").collect { new Double(it.split("\\|")[1]) }.toArray()
	  x << vecvec
	  dim = vecvec.size()
	  labels << v
	}
	/*    if (vmap[v]) { // found a vector for that one
	      x << vmap[v].toArray()
	      dim = vmap[v].size()
	      labels << v
	      }*/
      }
      jMap.results.bindings << cm
      first = false
    }

    TSne tsne = new SimpleTSne()
    def conf = TSneUtils.buildConfig((double[][])x.toArray(), 2, dim, 20.0, 1000)
    def y = tsne.tsne(conf)

    def tMap = [:]
    y.eachWithIndex { e, i ->
      def label = labels[i]
      tMap[label] = e
    }

    jMap.results.bindings = jMap.results.bindings.collect { m ->
      def m2 = [:]
      m.each { k, v ->
	if (v.value in labels) {
	  def coord = tMap[v.value]
	  def nkeyx = k+"_coord_x"
	  def nkeyy = k+"_coord_y"
	  m2[nkeyx] = [:]
	  m2[nkeyx].type = "double"
	  m2[nkeyx].value = coord[0]
	  m2[nkeyy] = [:]
	  m2[nkeyy].type = "double"
	  m2[nkeyy].value = coord[1]
	} 
      }
      m + m2
    }
    // Add the meta-data to turn this from a dumb API into a Smart API
    jMap."@context" = "//purl.org/smartapi/smartapi.jsonld"
    jMap."smartapi:meta" = [
      "prov:wasGeneratedBy": "https://github.com/bio-ontology-research-group/lodvectors/blob/master/tsne.groovy",
      "prov:generatedAt": new Date(),
      "smartapi:errors": [],
      "smartapi:warnings": [ "North Korean missile incoming." ],
      "smartapi:resultCount": jMap.results.bindings.size(),
      "smartapi:URLcalled": request.getRequestURL()+"?"+request.getQueryString()
    ]

    Expando exp = new Expando()
    exp.query = sparqlQuery
    //exp.result = l

    def builder = new JsonBuilder(jMap)
    response.contentType = 'application/json'
    println builder.toPrettyString()
  } else {
    jMap = [:]
    jMap."@context" = "//purl.org/smartapi/smartapi.jsonld"
    jMap."smartapi:meta" = [
      "prov:wasGeneratedBy": "https://github.com/bio-ontology-research-group/lodvectors/blob/master/tsne.groovy",
      "prov:generatedAt": new Date(),
      "smartapi:errors": [],
      "smartapi:warnings": [ "No results found." ],
      "smartapi:resultCount": 0,
      "smartapi:URLcalled": request.getRequestURL()+"?"+request.getQueryString()
    ]
    def builder = new JsonBuilder(jMap)
    response.contentType = 'application/json'
    println builder.toPrettyString()
  }
} catch (Exception E) {
  jMap = [:]
  jMap."@context" = "//purl.org/smartapi/smartapi.jsonld"
  jMap."smartapi:meta" = [
    "prov:wasGeneratedBy": "https://github.com/bio-ontology-research-group/lodvectors/blob/master/tsne.groovy",
    "prov:generatedAt": new Date(),
    "smartapi:errors": [E.getMessage()],
    "smartapi:warnings": [ ],
    "smartapi:resultCount": 0,
    "smartapi:URLcalled": request.getRequestURL()+"?"+request.getQueryString()
  ]
  def builder = new JsonBuilder(jMap)
  response.contentType = 'application/json'
  println builder.toPrettyString()
}

/*
  sparql.each sparqlQuery, {
  v = vmap[protein]
  if (v != null) {
  x << v.toArray()
  dim = v.size()
  labels << go
  proteins << protein

  // similarity computation, put in separate servlet
  def res = []
  vmap.each { k, v2 ->
  Expando exp = new Expando()
  exp.k = k
  exp.val = cosine(v, v2)
  res << exp
  }
  res = res.sort { it.val }.reverse()
  println "The 5 most similar proteins for $protein are " + res[0..5]
    
  }
  }
*/
