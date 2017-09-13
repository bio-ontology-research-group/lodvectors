@Grab(group='com.github.albaker', module='GroovySparql', version='0.9.0')
@Grab(group='com.jujutsu.tsne', module='tsne-demos', version='2.4.0')

import groovy.json.*

import groovy.sparql.*
import com.jujutsu.tsne.barneshut.*
import com.jujutsu.tsne.*
import com.jujutsu.utils.*
import java.util.stream.*
import org.math.plot.*
import org.math.plot.plots.*

def sparql = new Sparql(endpoint:"http://sparql.uniprot.org/")

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

def vmap = context.getAttribute('vmap')

def sparqlQuery = request.getParameter('query')

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
    cm."$k"."type" = (v instanceof String)?"uri":"literal"
    if (vmap[v]) { // found a vector for that one
      x << vmap[v].toArray()
      dim = vmap[v].size()
      labels << v
    }
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
  m = m.collectEntries { k, v ->
    if (v.value in labels) {
      def coord = tMap[v.value]
      v.coord = [:]
      v.coord.x = coord[0]
      v.coord.y = coord[1]
    } 
    [k, v]
  }
}

Expando exp = new Expando()
exp.query = sparqlQuery
//exp.result = l

def builder = new JsonBuilder(jMap)
response.contentType = 'application/json'
println builder.toPrettyString()


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
