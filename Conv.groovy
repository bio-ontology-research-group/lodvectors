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
println vmap
