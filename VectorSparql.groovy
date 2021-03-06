@Grab(group='com.github.albaker', module='GroovySparql', version='0.9.0')
@Grab(group='com.jujutsu.tsne', module='tsne-demos', version='2.4.0')

import groovy.sparql.*
import com.jujutsu.tsne.barneshut.*
import com.jujutsu.tsne.*
import com.jujutsu.utils.*

import org.math.plot.*
import org.math.plot.plots.*

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


def sparql = new Sparql(endpoint:"http://sparql.uniprot.org/")

def qProt2Cell = """
PREFIX up:<http://purl.uniprot.org/core/> 
PREFIX ec:<http://purl.uniprot.org/enzyme/> 
PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> 
PREFIX owl:<http://www.w3.org/2002/07/owl#> 
PREFIX bibo:<http://purl.org/ontology/bibo/> 
PREFIX dc:<http://purl.org/dc/terms/> 
PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> 
PREFIX faldo:<http://biohackathon.org/resource/faldo#> 
PREFIX keywords:<http://purl.uniprot.org/keywords/> 
PREFIX uniprotkb:<http://purl.uniprot.org/uniprot/> 
PREFIX taxon:<http://purl.uniprot.org/taxonomy/> 
PREFIX go:<http://purl.obolibrary.org/obo/GO_000>
PREFIX skos:<http://www.w3.org/2004/02/skos/core#> 
SELECT ?protein ?go
WHERE
{
  		VALUES ?go {go:5618
go:9507
go:5856
go:5829
go:5783
go:5768
go:5615
go:5794
go:5764
go:5739
go:5634
go:5777
go:5886
go:5773 }
		?protein a up:Protein .
   {
		?protein up:classifiedWith ?go .
    } UNION {
     ?protein up:classifiedWith|^rdfs:subClassOf ?go .
     }
  
  		?protein up:organism taxon:9606 .
} LIMIT 500
"""

def query = """

"""

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



def x = []
def dim = 0
def labels = []
sparql.each qProt2Cell, {
  v = vmap[protein]
  if (v != null) {
    x << v.toArray()
    dim = v.size()
    labels << go

    /*
    def res = []
    vmap.each { k, v2 ->
      Expando exp = new Expando()
      exp.k = k
      exp.val = cosine(v, v2)
      res << exp
    }
    res = res.sort { it.val }.reverse()
    println "The 5 most similar proteins for $protein are " + res[0..5]
    */
  }
}
TSne tsne = new SimpleTSne()
def conf = TSneUtils.buildConfig((double[][])x.toArray(), 2, dim, 20.0, 1500)
def y = tsne.tsne(conf)

Plot2DPanel plot = new Plot2DPanel();
ColoredScatterPlot setosaPlot = new ColoredScatterPlot("setosa", y, (String[])labels.toArray());
plot.plotCanvas.setNotable(true);
plot.plotCanvas.setNoteCoords(true);
plot.plotCanvas.addPlot(setosaPlot);
FrameView plotframe = new FrameView(plot);
//plotframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
plotframe.setVisible(true);

println y
